package dociq_backend.infrastructure.worker;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * The Dispatcher — pure orchestrator. Zero business logic here.
 *
 * Responsibilities:
 * 1. On startup: trigger stuck job recovery (delegates to JobRecoveryService)
 * 2. Every 5s: claim pending jobs (delegates to JobClaimService)
 * 3. Hand claimed jobs to async worker threads (delegates to AsyncDocumentProcessor)
 *
 * WHY NO @Transactional HERE:
 * - recoverStuckJobs(): @PostConstruct fires before proxy created → use separate bean
 * - pollAndDispatch(): must NOT be @Transactional — we want claim transaction
 *   to COMMIT before async threads start. If this was @Transactional,
 *   the claim and async processing would share a transaction context.
 * - claimPendingJobs(): was self-invocation → moved to JobClaimService
 *
 * WHY NO @Async HERE:
 * - processJobAsync() was self-invocation → moved to AsyncDocumentProcessor
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentJobDispatcher {

    private final JobRecoveryService jobRecoveryService;     // fixes @PostConstruct trap
    private final JobClaimService jobClaimService;           // fixes self-invocation trap
    private final AsyncDocumentProcessor asyncProcessor;     // fixes @Async self-invocation

    @Value("${dociq.jobs.batch-size:5}")
    private int batchSize;

    /**
     * Startup: reset any jobs stuck as RUNNING from previous server crash.
     *
     * Delegates to JobRecoveryService because:
     * @PostConstruct fires BEFORE Spring creates this bean's proxy.
     * @Transactional on THIS @PostConstruct = silent no-op (proxy doesn't exist yet).
     * JobRecoveryService IS fully proxied when called from here → @Transactional works.
     */
    @PostConstruct
    public void onStartup() {
        log.info("DocIQ Job Engine starting up...");
        jobRecoveryService.resetStuckJobs(); // external call → proxy hit → ✅
        log.info("Job Engine ready. Polling every 5 seconds.");
    }

    /**
     * Main dispatch loop. Runs every 5 seconds after previous execution completes.
     *
     * fixedDelay (not fixedRate): waits 5s AFTER the method finishes.
     * If a batch takes 2s, next poll fires at T+7s, not T+5s.
     * Prevents overlapping dispatches under slow DB conditions.
     *
     * NOT @Transactional: claim transaction must COMMIT before async threads start.
     */
    @Scheduled(fixedDelayString = "${dociq.jobs.poll-interval-ms:5000}")
    public void pollAndDispatch() {
        // Step 1: Atomically claim pending jobs (fast, ~5ms, releases DB connection)
        List<UUID> claimedJobIds = jobClaimService.claimPendingJobs();

        if (claimedJobIds.isEmpty()) {
            return; // nothing to do — silent exit, no log spam
        }

        log.info("Dispatching {} job(s) to worker thread pool", claimedJobIds.size());

        // Step 2: Submit each claimed job to the async thread pool (non-blocking)
        for (UUID jobId : claimedJobIds) {
            asyncProcessor.processJobAsync(jobId); // returns immediately — @Async ✅
        }

        // pollAndDispatch() returns in ~5ms total.
        // Actual processing happens in background on doc-worker threads.
    }
}