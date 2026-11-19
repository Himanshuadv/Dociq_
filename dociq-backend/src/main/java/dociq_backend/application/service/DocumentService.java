// application/service/DocumentService.java

/*

Step 7: The File-Before-Transaction Problem
There is one subtle issue in the design above: **file is written to disk before
the transaction commits.** If the transaction rolls back (e.g., DB is down),
the file sits on disk orphaned.

For v1.0 this is acceptable — the orphaned file is harmless and the upload
simply returns a 500 error. For production, add cleanup:





 */




package dociq_backend.application.service;

import dociq_backend.api.exception.FileValidationException;
import dociq_backend.application.dto.response.DocumentResponse;
import dociq_backend.domain.model.Document;
import dociq_backend.domain.model.Job;
import dociq_backend.domain.model.User;
import dociq_backend.domain.repository.DocumentRepository;
import dociq_backend.domain.repository.JobRepository;
import dociq_backend.domain.repository.UserRepository;
import dociq_backend.infrastructure.storage.LocalFileStorageService;
import dociq_backend.infrastructure.storage.MimeTypeUtils;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final LocalFileStorageService localFileStorageService;
    private final DocumentRepository documentRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    // Inject the value from application.yml
    @Value("${dociq.jobs.max_attempts:3}")
    private int maxAttempts;
    private static final Tika TIKA = new Tika(); // thread-safe, reuse across requests

    /**
     * Uploads a document and atomically queues it for async processing.
     *
     * Transaction scope: Document save + Job save are ONE transaction.
     * Either both succeed or both roll back. No partial state.
     *
     * File write to disk happens BEFORE the transaction.
     * If the transaction rolls back, the orphaned file is cleaned up.
     */
    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, String title, String userEmail) {

        // ── 1. Cheap validations first (no stream read, no DB) ──────────────
        validateMetadata(file, title);

        // ── 2. Detect true MIME type from magic bytes (reads stream once) ───
        String detectedMimeType = detectMimeType(file);

        // ── 3. Validate MIME against allowlist ──────────────────────────────
        if (!MimeTypeUtils.ALLOWED_MIME_TYPES.contains(detectedMimeType)) {
            throw new FileValidationException(
                    "File type '" + detectedMimeType + "' is not supported. Allowed: PDF, DOCX."
            );
        }

        // ── 4. Resolve authenticated user ───────────────────────────────────
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found for email: " + userEmail
                        // Should never happen if JWT auth is working correctly
                ));

        // ── 5. Generate document ID — shared key for DB and storage ─────────
        UUID documentId = UUID.randomUUID();

        // ── 6. Write file to disk (outside transaction — filesystem write) ───
        //    If this fails, no DB records exist yet. Clean failure.
        //    If DB transaction fails AFTER this, we clean up the orphaned file.\

        String filePath = null;

        try{
            filePath = localFileStorageService.store(file, documentId, detectedMimeType);

            // ── 7. Persist Document entity ───────────────────────────────────────
            String originalFilename = file.getOriginalFilename() != null
                    ? file.getOriginalFilename() : "unknown";

            Document document = Document.builder()
                    .id(documentId)
                    .userId(user.getId())
                    .originalFilename(originalFilename)
                    .storedFilename(documentId + MimeTypeUtils.resolveExtension(detectedMimeType))
                    .filePath(filePath)
                    .fileSizeBytes(file.getSize())
                    .mimeType(detectedMimeType)
                    .status("UPLOADED")
                    .domain("GENERAL")
                    .build();

            Document savedDocument = documentRepository.save(document);
            // ── 8. Create Job atomically with Document ───────────────────────────
            //    SAME transaction. If job save fails, document save rolls back too.
            //    A Document without a Job must never exist.
            Job job = Job.createForDocument(savedDocument,maxAttempts);
            Job savedJob = jobRepository.save(job);

            log.info("Document uploaded and job queued: documentId={}, jobId={}, user={}",
                    savedDocument.getId(), savedJob.getId(), userEmail);

            // ── 9. Return response with both IDs ─────────────────────────────────
            return new DocumentResponse(
                    savedDocument.getId(),
                    savedDocument.getUserId(),
                    savedJob.getId(),       // client polls this
                    savedDocument.getOriginalFilename(),
                    savedDocument.getFileSizeBytes(),
                    savedDocument.getStatus(),
                    savedDocument.getMimeType(),
                    savedDocument.getCreatedAt(),
                    savedDocument.getUpdatedAt()
            );

        }catch(Exception ex){
            // Rollback: clean up orphaned file if it was written
            if (filePath != null) {
                localFileStorageService.delete(documentId,detectedMimeType);
                log.warn("Rolled back file storage for documentId={} due to: {}", documentId, ex.getMessage());
            }
            throw ex; // re-throw so @Transactional also rolls back DB
        }







    }

    // ─────────────────────────── PRIVATE HELPERS ─────────────────────────────

    private void validateMetadata(MultipartFile file, String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new FileValidationException("Document title cannot be empty.");
        }
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("Cannot upload an empty file.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new FileValidationException("File size exceeds the 10MB limit.");
        }
    }

    private String detectMimeType(MultipartFile file) {
        try (InputStream is = file.getInputStream()) { // ← fixed: try-with-resources
            return TIKA.detect(is);
        } catch (IOException e) {
            throw new FileValidationException("Could not read file for type detection.");
        }
    }
}