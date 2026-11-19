package dociq_backend.infrastructure.worker;

import dociq_backend.domain.model.Job;
import dociq_backend.domain.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * WHY THIS EXISTS:
 * DocumentJobDispatcher had claimJobs() calling itself (this.claimJobs()).
 * Self-invocation bypasses the Spring proxy → @Transactional silently ignored.
 * Without a transaction, SELECT FOR UPDATE SKIP LOCKED has NO locking effect.
 * Two workers running simultaneously would claim the SAME jobs → duplicate processing.
 *
 * Fix: extract to this separate @Service bean.
 * DocumentJobDispatcher calls THIS bean (external call) → proxy is hit → ✅
 *
 * This transaction is intentionally SHORT:
 * - Acquires DB connection
 * - Locks rows with SELECT FOR UPDATE SKIP LOCKED
 * - Marks jobs RUNNING
 * - Commits and releases DB connection
 * Total time: < 10ms
 * DB connection is NOT held during the actual document processing (30s+).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobClaimService {

    private final JobRepository jobRepository;

    @Value("${dociq.jobs.batch-size:5}")
    private int batchSize;

    /**
     * Atomically claims pending jobs for processing.
     *
     * SELECT FOR UPDATE: locks the selected rows for this transaction.
     * SKIP LOCKED: if another worker already locked a row, skip it (don't wait).
     * Result: each worker instance gets a UNIQUE set of jobs. Zero overlap.
     *
     * @return List of job IDs that have been claimed (status set to RUNNING).
     *         Empty list if no pending jobs exist.
     */
    @Transactional  // ← works correctly: external call from Dispatcher → proxy hit ✅
    public List<UUID> claimPendingJobs() {
        List<Job> pendingJobs = jobRepository.findAndLockPendingJobs(batchSize);

        if (pendingJobs.isEmpty()) {
            return List.of();
        }

        List<UUID> claimedIds = pendingJobs.stream()
                .map(job -> {
                    job.markRunning();        // PENDING → RUNNING (state machine)
                    jobRepository.save(job);  // flush to DB within this transaction
                    return job.getId();
                })
                .collect(Collectors.toList());

        log.info("Claimed {} job(s): {}", claimedIds.size(), claimedIds);

        return claimedIds;
        // ↑ Transaction COMMITS here.
        // DB locks released. Jobs are officially RUNNING in DB.
        // Safe to hand off to async workers — no DB connection held.
    }
}