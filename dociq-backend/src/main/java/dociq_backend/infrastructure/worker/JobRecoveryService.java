package dociq_backend.infrastructure.worker;

import dociq_backend.domain.enums.JobStatus;
import dociq_backend.domain.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * WHY THIS EXISTS:
 * @PostConstruct fires BEFORE Spring creates the proxy wrapper.
 * So @Transactional on a @PostConstruct method = silent no-op.
 * The fix: delegate to THIS separate @Service bean.
 * When DocumentJobDispatcher calls this bean from @PostConstruct,
 * the call passes through THIS bean's proxy — @Transactional works. ✅
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobRecoveryService {

    private final JobRepository jobRepository;

    // 10 minutes: if a job has been RUNNING for 10+ min, server crashed mid-process
    private static final int STUCK_JOB_THRESHOLD_MINUTES = 10;

    /**
     * Called once on application startup.
     * Resets any jobs stuck as RUNNING (server crashed during processing).
     * @Transactional works here because this is a proper proxied @Service bean.

     Why do we need to be tranctional here?
    In Spring Data JPA, any query that modifies data (UPDATE, DELETE, INSERT) strictly requires an active transaction.
    Spring treats SELECT queries casually, but it treats write operations like highly secure vault access.
    If you don't declare a transaction wrapper, Spring refuses to execute the @Modifying query.



     */
    @Transactional
    public void resetStuckJobs() {
        Instant cutoff = Instant.now()
                .minusSeconds(STUCK_JOB_THRESHOLD_MINUTES * 60L);

        int resetCount = jobRepository.resetStuckRunningJobs(cutoff, JobStatus.PENDING, JobStatus.RUNNING);

        if (resetCount > 0) {
            log.warn(
                    "Startup recovery: reset {} job(s) stuck as RUNNING (started before {}) → PENDING",
                    resetCount, cutoff
            );
        } else {
            log.info("Startup recovery: no stuck jobs found. System is clean.");
        }
    }
}