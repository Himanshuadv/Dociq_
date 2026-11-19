// application/dto/response/JobStatusResponse.java
package dociq_backend.application.dto.response;
import dociq_backend.domain.model.Job;

import java.time.Instant;
import java.util.UUID;

public record JobStatusResponse(
        UUID jobId,
        UUID documentId,
        String status,
        int attemptCount,
        int maxAttempts,
        String errorMessage,
        Instant startedAt,
        Instant completedAt,
        Instant createdAt
) {
    public static JobStatusResponse from(Job job) {
        return new JobStatusResponse(
                job.getId(),
                job.getDocument().getId(),
                job.getStatus().name(),
                job.getAttemptCount(),
                job.getMaxAttempts(),
                job.getErrorMessage(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getCreatedAt()
        );
    }
}