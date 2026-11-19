// domain/model/Job.java

package dociq_backend.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import dociq_backend.domain.enums.JobStatus;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Lazy: we only need the document for processing, not for status checks
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    @Column(nullable = false)
    private int attemptCount;

    @Column(nullable = false)
    private int maxAttempts;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Instant startedAt;
    private Instant completedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    // ──────────────────────────── FACTORY METHOD ────────────────────────────

    /**
     * Creates a new job ready to be queued.
     * Always use this instead of the builder directly — ensures correct defaults.
     */
    public static Job createForDocument(Document document, Integer maxAttempts) {
        return Job.builder()
                .document(document)
                .status(JobStatus.PENDING)
                .attemptCount(0)
                .maxAttempts(maxAttempts)
                .build();
    }

    // ─────────────────────────── STATE TRANSITIONS ───────────────────────────
    // These methods enforce the state machine.
    // Illegal transitions throw immediately — no silent data corruption.

    /**
     * PENDING → RUNNING
     * Called when a worker picks up this job.
     */
    public void markRunning() {
        requireStatus(JobStatus.PENDING, "start");
        this.status = JobStatus.RUNNING;
        this.startedAt = Instant.now();
        this.attemptCount++;
    }

    /**
     * RUNNING → COMPLETED
     * Called when processing finishes successfully.
     */
    public void markCompleted() {
        requireStatus(JobStatus.RUNNING, "complete");
        this.status = JobStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.errorMessage = null;
    }

    /**
     * RUNNING → FAILED
     * Called when processing throws an exception.
     */
    // Only the corrected methods shown — rest of Job.java unchanged

    /**
     * RUNNING → FAILED
     */
    public void markFailed(String errorMessage) {
        requireStatus(JobStatus.RUNNING, "fail");
        this.status = JobStatus.FAILED;
        this.errorMessage = errorMessage != null
                ? errorMessage.substring(0, Math.min(errorMessage.length(), 1000))
                : "Unknown error";
    }

    /**
     * FAILED → PENDING (for retry)
     *
     * BUG IN PREVIOUS VERSION:
     * markRetry() was calling canRetry() AFTER markFailed() had already
     * incremented attemptCount. The check was: attemptCount < maxAttempts.
     * But markFailed() doesn't increment count — markRunning() does.
     * The logic was correct but the ordering was confusing.
     *
     * FIXED: explicit guard with clear error message.
     * Caller (DocumentProcessingService.handleFailure) checks canRetry()
     * BEFORE calling markRetry() — fail-fast, no silent state corruption.
     */
    public void markRetry() {
        requireStatus(JobStatus.FAILED, "retry");
        // Guard: caller should have checked canRetry() before calling this
        if (!canRetry()) {
            throw new IllegalStateException(String.format(
                    "Job %s cannot be retried: %d/%d attempts exhausted.",
                    id, attemptCount, maxAttempts
            ));
        }
        this.status = JobStatus.PENDING;
        this.errorMessage = null; // clear previous error — fresh start
    }

    /**
     * Returns true if this job has remaining retry attempts.
     * Check this BEFORE calling markRetry().
     *
     * attemptCount is incremented by markRunning(), not markFailed().
     * So after 3 RUNNING→FAILED cycles: attemptCount=3, maxAttempts=3 → false.
     */
    public boolean canRetry() {
        return this.attemptCount < this.maxAttempts;
    }

    // ───────────────────────────── PRIVATE ───────────────────────────────────

    private void requireStatus(JobStatus required, String operation) {
        if (this.status != required) {
            throw new IllegalStateException(String.format(
                    "Cannot %s job %s: expected status %s but was %s",
                    operation, id, required, status
            ));
        }
    }
}