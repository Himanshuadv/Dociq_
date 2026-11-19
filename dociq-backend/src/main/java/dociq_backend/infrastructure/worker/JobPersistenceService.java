package dociq_backend.infrastructure.worker;

import dociq_backend.application.dto.analysis.AnalysisResult;
import dociq_backend.domain.model.Document;
import dociq_backend.domain.model.Job;
import dociq_backend.domain.repository.AnalysisResultRepository;
import dociq_backend.domain.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Owns ALL database operations for the job processing pipeline.
 *
 * WHY THIS EXISTS:
 * DocumentProcessingService.process() called loadJob(), persistResults(),
 * handleFailure() on 'this' — self-invocation bypasses proxy.
 * @Transactional was silently ignored on all three methods.
 *
 * Fix: move all @Transactional DB methods here.
 * DocumentProcessingService calls THIS bean (external call) → proxy hit ✅
 *
 * TRANSACTION DESIGN:
 * Each method has its own SHORT transaction.
 * No single transaction spans the entire pipeline.
 * DB connection held for < 50ms per method.
 * Tika I/O and LLM calls happen BETWEEN these methods — no DB connection held.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobPersistenceService {

    private final JobRepository jobRepository;
    private final AnalysisResultRepository analysisResultRepository;

    /**
     * Load job + document in one JOIN query.
     * @Transactional(readOnly = true): DB connection acquired, SELECT, released.
     * readOnly hint: DB driver can optimize (no dirty tracking, no flush).
     */
    @Transactional(readOnly = true)
    public Job loadJobWithDocument(UUID jobId) {
        return jobRepository.findByIdWithDocument(jobId)
                .orElseGet(() -> {
                    log.error("Job not found in DB: {}. Cannot process.", jobId);
                    return null;
                });
    }

    /**
     * Persist analysis results and mark job COMPLETED atomically.
     *
     * Both the INSERT into analysis_results AND the UPDATE to jobs
     * are in ONE transaction. Either both succeed or both rollback.
     * You never get a job marked COMPLETED with no results saved.
     */
    @Transactional
    public void persistResultsAndComplete(Job job, Document document,
                                          AnalysisResult result) {
        // Build and save analysis result entity
        dociq_backend.domain.model.AnalysisResult entity =
                dociq_backend.domain.model.AnalysisResult.builder()
                        .document(document)
                        .job(job)
                        .summary(result.summary())
                        .keyPoints(result.keyPoints())
                        .riskFlags(result.riskFlags())
                        .domain(result.domain())
                        .build();

        analysisResultRepository.save(entity);

        // Mark job completed — same transaction, atomic with result save
        job.markCompleted();
        jobRepository.save(job);

        log.info("Results persisted and job marked COMPLETED: job={} riskFlags={}",
                job.getId(), result.riskFlags().size());
    }

    /**
     * Mark job FAILED, then decide: reset for retry or leave permanently FAILED.
     *
     * Called only on exception in the pipeline.
     * One transaction: read job + update status + save.
     */
    @Transactional
    public void markFailedOrRetry(UUID jobId, String errorMessage) {
        jobRepository.findById(jobId).ifPresentOrElse(
                job -> {
                    job.markFailed(errorMessage);

                    if (job.canRetry()) {
                        log.warn("Job {} → retry. Attempt {}/{} failed. Reason: {}",
                                jobId, job.getAttemptCount(), job.getMaxAttempts(), errorMessage);
                        job.markRetry(); // FAILED → PENDING for next poll cycle
                    } else {
                        log.error("Job {} → permanently FAILED after {}/{} attempts. Reason: {}",
                                jobId, job.getAttemptCount(), job.getMaxAttempts(), errorMessage);
                        // Stays FAILED. Operator must investigate via admin dashboard.
                    }

                    jobRepository.save(job);
                },
                () -> log.error("Cannot mark failure: job {} not found in DB", jobId)
        );
    }
}