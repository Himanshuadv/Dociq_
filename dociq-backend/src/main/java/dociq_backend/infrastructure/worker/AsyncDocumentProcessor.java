package dociq_backend.infrastructure.worker;

import dociq_backend.application.dto.analysis.AnalysisResult;
import dociq_backend.application.port.DocumentAnalyzer;
import dociq_backend.domain.enums.JobStatus;
import dociq_backend.domain.model.Document;
import dociq_backend.domain.model.Job;
import dociq_backend.domain.repository.AnalysisResultRepository;
import dociq_backend.domain.repository.JobRepository;
import dociq_backend.infrastructure.storage.LocalFileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * WHY THIS EXISTS AS A SEPARATE CLASS:
 * @Async only works when called from OUTSIDE the bean (external call through proxy).
 * If DocumentJobDispatcher called processJobAsync() on itself,
 * @Async would be ignored — method runs synchronously on the scheduler thread.
 * Scheduler thread blocked for 30s = no other jobs dispatched during that time.
 *
 * Fix: this is a SEPARATE @Component.
 * DocumentJobDispatcher → calls this bean → proxy intercepts → @Async works ✅
 * Method runs on "documentProcessingExecutor" thread pool, not scheduler thread.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AsyncDocumentProcessor {

    private final DocumentProcessingService processingService;

    /**
     * Submits a job to the document processing thread pool.
     * Returns IMMEDIATELY — the actual work happens on a worker thread.
     *
     * Thread pool: "documentProcessingExecutor" (defined in AsyncConfig)
     * - corePoolSize: 2  → 2 documents always processed in parallel
     * - maxPoolSize: 4   → burst to 4 under high load
     * - queueCapacity: 50 → buffer for 50 waiting jobs
     *
     * @param jobId The UUID of the job to process (already marked RUNNING in DB)
     */
    @Async("documentProcessingExecutor")
    public void processJobAsync(UUID jobId) {
        log.info("Worker thread [{}] starting job={}",
                Thread.currentThread().getName(), jobId);
        processingService.process(jobId);
        // Note: exceptions here are caught by DocumentProcessingService.
        // Uncaught exceptions fall to AsyncConfig.getAsyncUncaughtExceptionHandler()
    }
}