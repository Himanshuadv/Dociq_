package dociq_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Thread pool for document processing workers.
     *
     * corePoolSize=2:    2 threads always alive, process 2 docs in parallel
     * maxPoolSize=4:     burst to 4 under high load
     * queueCapacity=50:  buffer 50 jobs before rejecting
     * threadNamePrefix:  visible in logs/profilers as "doc-worker-1", "doc-worker-2"
     * CallerRunsPolicy:  if pool+queue full, caller thread processes it (no job dropped)
     */
    @Bean("documentProcessingExecutor")
    public ThreadPoolTaskExecutor documentProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("doc-worker-");
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true); // graceful shutdown
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Default async executor (used when @Async has no explicit executor name).
     */
    @Override
    public Executor getAsyncExecutor() {
        return documentProcessingExecutor();
    }

    /**
     * Catches exceptions that escape @Async methods uncaught.
     * This is the last resort — DocumentProcessingService should catch its own exceptions.
     * But if something unexpected slips through, this prevents silent failures.
     *
     * Note: job stays as RUNNING in DB when this fires.
     * The @PostConstruct recovery on next startup will reset it to PENDING.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            // Extract jobId from method params for better logging
            UUID jobId = null;
            if (params != null && params.length > 0 && params[0] instanceof UUID) {
                jobId = (UUID) params[0];
            }

            log.error(
                    "UNCAUGHT @Async exception in {}() for jobId={}: {}",
                    method.getName(), jobId, ex.getMessage(), ex
            );

            // The job will remain RUNNING in DB.
            // Next server startup recoverStuckJobs() will reset it to PENDING.
            // For immediate recovery in production: inject JobRepository here
            // and mark it FAILED directly.
        };
    }
}