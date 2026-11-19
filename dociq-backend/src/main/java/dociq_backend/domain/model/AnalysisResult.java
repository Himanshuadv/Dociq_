package dociq_backend.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Persisted result of an AI analysis run on a Document.
 *
 * DESIGN DECISIONS:
 *
 * 1. keyPoints and riskFlags stored as JSON columns (PostgreSQL jsonb).
 *    - Alternative was a separate join table (analysis_key_points, analysis_risk_flags).
 *    - JSON chosen because: these lists are always read/written together with the
 *      parent entity, never queried individually, and vary in length (0-50 items).
 *    - Trade-off: cannot do SQL WHERE on individual key points. Acceptable for Phase 2.
 *    - Future: if full-text search on key points is needed, move to ElasticSearch.
 *
 * 2. One-to-One with Job (not One-to-Many).
 *    - One document → one analysis job → one result. Reprocessing creates a NEW job
 *      and UPDATES (replaces) the existing result row. Job history is preserved in
 *      the jobs table; only the latest analysis result is kept here.
 *
 * 3. One-to-One with Document (unique constraint on document_id).
 *    - Enforced at DB level: UNIQUE(document_id).
 *    - At application level: JobPersistenceService calls upsert via
 *      saveOrUpdateByDocumentId() so reprocessing overwrites cleanly.
 *
 * 4. confidenceScore: nullable Double.
 *    - Not all analyzers return a confidence score (mock doesn't).
 *    - Nullable is cleaner than defaulting to 0.0 which is misleading.
 *
 * 5. processingDurationMs: how long the full pipeline took.
 *    - Useful for observability dashboard (Phase 4).
 *    - Set by JobPersistenceService before saving.
 */
@Entity
@Table(
        name = "analysis_results",
        indexes = {
                @Index(name = "idx_analysis_results_document_id", columnList = "document_id"),
                @Index(name = "idx_analysis_results_job_id",      columnList = "job_id"),
                @Index(name = "idx_analysis_results_domain",      columnList = "domain"),
                @Index(name = "idx_analysis_results_created_at",  columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AnalysisResult {

    // ─── Identity ──────────────────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // ─── Relationships ─────────────────────────────────────────────────────

    /**
     * The document this result belongs to.
     * UNIQUE constraint at DB level prevents duplicate results per document.
     * FetchType.LAZY: we rarely need the full document when reading results.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_analysis_results_document"))
    private Document document;

    /**
     * The job that produced this result.
     * Allows tracing back: result → job → attempt history.
     * LAZY: only needed in admin/debug views.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_analysis_results_job"))
    private Job job;

    // ─── Core Analysis Output ──────────────────────────────────────────────

    /**
     * AI-generated plain-text summary of the document.
     * TEXT column: no length limit (documents can be large).
     */
    @Column(name = "summary", columnDefinition = "TEXT", nullable = false)
    private String summary;

    /**
     * Key points extracted from the document.
     * Stored as PostgreSQL jsonb — fast binary JSON with GIN index support.
     * Mapped via Hibernate's @JdbcTypeCode(SqlTypes.JSON).
     *
     * Example: ["Clause 4.2 limits liability to $10,000",
     *           "Arbitration required for all disputes"]
     */
    @Column(name = "key_points", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private List<String> keyPoints = new ArrayList<>();

    /**
     * Risk flags identified by the AI analyzer.
     * Stored as jsonb — same rationale as keyPoints.
     *
     * Example: ["AUTO_RENEWAL clause detected",
     *           "GDPR data transfer to non-EU countries"]
     */
    @Column(name = "risk_flags", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private List<String> riskFlags = new ArrayList<>();

    // ─── Classification ────────────────────────────────────────────────────

    /**
     * Document domain/category (e.g., "legal", "medical", "financial").
     * Determined during analysis — may refine the upload-time domain.
     */
    @Column(name = "domain", nullable = false, length = 100)
    private String domain;

    // ─── Quality / Observability ───────────────────────────────────────────

    /**
     * Analyzer confidence score [0.0, 1.0].
     * Nullable: mock analyzer and some real analyzers may not provide this.
     */
    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    /**
     * Total wall-clock time for the full processing pipeline (ms).
     * Includes: text extraction + LLM call + DB write.
     * Used in Phase 4 observability dashboard.
     */
    @Column(name = "processing_duration_ms")
    private Long processingDurationMs;

    /**
     * Number of words in the extracted document text.
     * Denormalized here for quick display — avoids re-reading file.
     */
    @Column(name = "word_count")
    private Integer wordCount;

    // ─── Audit ────────────────────────────────────────────────────────────

    /**
     * When this result was first created.
     * @CreationTimestamp: set by Hibernate on INSERT, never updated.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * When this result was last updated (e.g., reprocessing replaced old result).
     * Managed manually — set by JobPersistenceService before save.
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    // ─── Factory / Domain Methods ──────────────────────────────────────────

    /**
     * Factory method — preferred over constructor.
     * Ensures required fields are always set together.
     * Called by JobPersistenceService.persistResultsAndComplete().
     */
    public static AnalysisResult create(
            Document document,
            Job job,
            String summary,
            List<String> keyPoints,
            List<String> riskFlags,
            String domain,
            BigDecimal confidenceScore,
            Integer wordCount,
            long processingDurationMs
    ) {
        Instant now = Instant.now();
        return AnalysisResult.builder()
                .document(document)
                .job(job)
                .summary(summary)
                .keyPoints(keyPoints != null ? keyPoints : new ArrayList<>())
                .riskFlags(riskFlags != null ? riskFlags : new ArrayList<>())
                .domain(domain)
                .confidenceScore(confidenceScore)
                .wordCount(wordCount)
                .processingDurationMs(processingDurationMs)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    /**
     * Called when re-processing replaces an existing result.
     * Updates fields in-place so the same DB row is updated (same UUID).
     */
    public void replaceWith(
            Job newJob,
            String summary,
            List<String> keyPoints,
            List<String> riskFlags,
            String domain,
            BigDecimal confidenceScore,
            Integer wordCount,
            long processingDurationMs
    ) {
        this.job = newJob;
        this.summary = summary;
        this.keyPoints = keyPoints != null ? keyPoints : new ArrayList<>();
        this.riskFlags = riskFlags != null ? riskFlags : new ArrayList<>();
        this.domain = domain;
        this.confidenceScore = confidenceScore;
        this.wordCount = wordCount;
        this.processingDurationMs = processingDurationMs;
        this.updatedAt = Instant.now();
    }

    /**
     * Convenience: does this result have any risk flags?
     */
    public boolean hasRisks() {
        return riskFlags != null && !riskFlags.isEmpty();
    }

    /**
     * Convenience: risk flag count (safe null check).
     */
    public int riskFlagCount() {
        return riskFlags == null ? 0 : riskFlags.size();
    }

    @Override
    public String toString() {
        return String.format(
                "AnalysisResult{id=%s, documentId=%s, domain=%s, risks=%d, words=%d}",
                id,
                document != null ? document.getId() : "null",
                domain,
                riskFlagCount(),
                wordCount != null ? wordCount : 0
        );
    }
}