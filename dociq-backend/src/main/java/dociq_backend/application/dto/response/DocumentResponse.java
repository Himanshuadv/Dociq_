package dociq_backend.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID documentId,
        UUID userId,
        UUID jobId,
        String originalFilename,
        Long fileSizeBytes,
        String status,
        String mimeType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {}