package dociq_backend.domain.repository;

import dociq_backend.domain.model.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AnalysisResult entity.
 *
 * QUERY DESIGN NOTES:
 *
 * findByDocumentId():
 *   The most common read — fetch result for a given document.
 *   Uses @Query with JOIN FETCH to avoid N+1 on document/job navigation.
 *   Returns Optional: result may not exist if job is still PENDING/RUNNING.
 *
 * saveOrUpdateByDocumentId():
 *   "Upsert" pattern for reprocessing.
 *   JobPersistenceService uses this instead of blindly calling save():
 *     - If result already exists → call existingResult.replaceWith(...) then save()
 *     - If not → call AnalysisResult.create(...) then save()
 *   This keeps the same UUID across reprocessing (no orphaned rows).
 *   The method is NOT in the repository itself because it requires domain logic
 *   (replaceWith) — it lives in JobPersistenceService.
 *
 * deleteByDocumentId():
 *   Used by document deletion endpoint (Phase 2 cleanup).
 *   @Modifying + @Transactional: bulk DELETE in one SQL statement.
 *
 * findAllByDomain():
 *   Used by admin analytics dashboard (Phase 5).
 *   No pagination here — see findAllByDomainPageable() for paginated version.
 *
 * findTopRiskDocuments():
 *   Native query for analytics: ranks documents by number of risk flags.
 *   json_array_length() is PostgreSQL-specific.
 *   Returns List<Object[]> — projected in service layer to DTO.
 */
@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, UUID> {

    // ── Primary Lookup ──────────────────────────────────────────────────────

    /**
     * Fetch result for a given document, with document + job eagerly loaded.
     * JOIN FETCH prevents N+1 when accessing result.getDocument() or result.getJob().
     * Called by: DocumentController.getAnalysisResult(), JobPersistenceService.
     */
    @Query("""
            SELECT ar FROM AnalysisResult ar
            JOIN FETCH ar.document
            JOIN FETCH ar.job
            WHERE ar.document.id = :documentId
            """)
    Optional<AnalysisResult> findByDocumentId(@Param("documentId") UUID documentId);

    /**
     * Fetch by job ID — useful for job status polling.
     * Called by: JobController.getJobResult().
     */
    @Query("""
            SELECT ar FROM AnalysisResult ar
            JOIN FETCH ar.document
            WHERE ar.job.id = :jobId
            """)
    Optional<AnalysisResult> findByJobId(@Param("jobId") UUID jobId);

    /**
     * Quick existence check — avoids loading the full entity.
     * Called by: JobPersistenceService to decide create vs. update.
     */
    boolean existsByDocumentId(UUID documentId);

    // ── Filtered Queries ────────────────────────────────────────────────────

    /**
     * All results for a specific user.
     * Called by: DocumentController.listMyResults(userId).
     * Note: no pagination — add Pageable parameter when result sets grow.
     */
    @Query("""
            SELECT ar FROM AnalysisResult ar
            JOIN FETCH ar.document d
            WHERE d.userId = :userId
            ORDER BY ar.createdAt DESC
            """)
    List<AnalysisResult> findAllByUserId(@Param("userId") UUID userId);

    /**
     * Results filtered by domain (e.g., "legal", "medical").
     * Called by: admin analytics endpoint.
     */
    @Query("""
            SELECT ar FROM AnalysisResult ar
            JOIN FETCH ar.document
            WHERE ar.domain = :domain
            ORDER BY ar.createdAt DESC
            """)
    List<AnalysisResult> findAllByDomain(@Param("domain") String domain);

    /**
     * Results with at least one risk flag.
     * Called by: risk dashboard (Phase 5).
     *
     * json_array_length() is PostgreSQL-specific.
     * Trade-off: native query ties us to PostgreSQL. Acceptable — we commit to PG.
     */
    @Query(value = """
            SELECT * FROM analysis_results
            WHERE json_array_length(risk_flags::json) > 0
            ORDER BY json_array_length(risk_flags::json) DESC, created_at DESC
            """, nativeQuery = true)
    List<AnalysisResult> findAllWithRisks();

    // ── Analytics Queries (Phase 5) ─────────────────────────────────────────

    /**
     * Count of results grouped by domain.
     * Returns List<Object[]>: [domain (String), count (Long)]
     * Projected to AnalyticsSummaryDTO in service layer.
     */
    @Query("""
            SELECT ar.domain, COUNT(ar)
            FROM AnalysisResult ar
            GROUP BY ar.domain
            ORDER BY COUNT(ar) DESC
            """)
    List<Object[]> countByDomain();

    /**
     * Average confidence score per domain.
     * Returns List<Object[]>: [domain (String), avgScore (Double)]
     * NULL domains excluded — only analyzers that set confidenceScore.
     */
    @Query("""
            SELECT ar.domain, AVG(ar.confidenceScore)
            FROM AnalysisResult ar
            WHERE ar.confidenceScore IS NOT NULL
            GROUP BY ar.domain
            """)
    List<Object[]> avgConfidenceByDomain();

    /**
     * Average processing duration per domain (ms).
     * Useful for SLA monitoring dashboard.
     */
    @Query("""
            SELECT ar.domain, AVG(ar.processingDurationMs)
            FROM AnalysisResult ar
            WHERE ar.processingDurationMs IS NOT NULL
            GROUP BY ar.domain
            ORDER BY AVG(ar.processingDurationMs) DESC
            """)
    List<Object[]> avgProcessingDurationByDomain();

    // ── Mutations ───────────────────────────────────────────────────────────

    /**
     * Hard delete a result by document ID.
     * Called when a document is deleted by the user.
     *
     * @Modifying: required for non-SELECT queries in Spring Data JPA.
     * Must be called within a @Transactional context (caller provides it).
     */
    @Modifying
    @Query("DELETE FROM AnalysisResult ar WHERE ar.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") UUID documentId);
}