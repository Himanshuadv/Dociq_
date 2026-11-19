package dociq_backend.domain.repository;

import dociq_backend.domain.enums.JobStatus;
import dociq_backend.domain.model.Job;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    /**
     * Core claim query.
     * FOR UPDATE: locks the selected rows.
     * SKIP LOCKED: skips rows locked by other transactions (other workers).
     * Result: each worker gets a UNIQUE set of jobs. No double-processing.
     *
     * Requires nativeQuery=true — SKIP LOCKED is not JPQL.
     * Requires @Transactional on caller — FOR UPDATE needs an active transaction.
     * Without @Transactional: no lock, race condition, duplicate jobs.
     */
    @Query(value = """
            SELECT * FROM jobs
            WHERE status = 'PENDING'
            ORDER BY created_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Job> findAndLockPendingJobs(@Param("limit") int limit);

    /**
     * Recovery query: jobs stuck as RUNNING (server crashed during processing).
     * cutoff = NOW() - 10 minutes.
     * Any job RUNNING before the cutoff was likely abandoned.
     */
    @Transactional
    @Modifying
    @Query("UPDATE Job j SET j.status = :pendingStatus, j.errorMessage = 'Reset: found stuck as RUNNING on startup' WHERE j.status = :runningStatus AND j.startedAt < :cutoff")
    int resetStuckRunningJobs(
            @Param("cutoff") Instant cutoff,
            @Param("pendingStatus") JobStatus pendingStatus,
            @Param("runningStatus") JobStatus runningStatus
    );

    /**
     * NEW: fetch job WITH its document in one query (no N+1).
     * DocumentProcessingService needs both job and document data.
     * Without JOIN FETCH: accessing job.getDocument() fires a second SELECT.
     */
    @Query("SELECT j FROM Job j JOIN FETCH j.document WHERE j.id = :jobId")
    Optional<Job> findByIdWithDocument(@Param("jobId") UUID jobId);

    Optional<Job> findByDocumentId(UUID documentId);
}