package dociq_backend.infrastructure.storage;

import dociq_backend.api.exception.StorageException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileStorageService implements StorageService {

    private final Path uploadBaseDir;
    private MimeTypeUtils mimeTypeUtils; // Note: Ensure this is properly injected/instantiated if not static

    public LocalFileStorageService(@Value("${file.upload-dir:uploads}") String uploadDirectory) {
        this.uploadBaseDir = Paths.get(uploadDirectory).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.uploadBaseDir);
            log.info("Storage initialized at: {}", this.uploadBaseDir);
        } catch (IOException e) {
            throw new StorageException("Cannot create upload directory", e);
        }
    }

    @Override
    public String store(MultipartFile file, UUID documentId, String mimeType) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("Cannot store an empty file.");
        }

        Path targetPath = buildSafeStoragePath(documentId, mimeType);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Failed to write file to disk.", e);
        }

        log.info("File stored at: {}", targetPath);
        return targetPath.toString();
    }

    @Override
    public String delete(UUID documentId, String detectedMimetype) {
        Path targetPath = buildSafeStoragePath(documentId, detectedMimetype);

        try {
            boolean deleted = Files.deleteIfExists(targetPath);
            if (deleted) {
                log.info("File deleted successfully at: {}", targetPath);
            } else {
                log.warn("File not found for deletion at: {}", targetPath);
            }
            return targetPath.toString(); // Returning the path to mirror the store() method's behavior
        } catch (IOException e) {
            log.error("Error deleting file at: {}", targetPath, e);
            throw new StorageException("Failed to delete file from disk.", e);
        }
    }

    private Path buildSafeStoragePath(UUID documentId, String mimeType) {
        // ✅ FIX: append the correct extension derived from the real MIME type
        String filename = documentId.toString() + MimeTypeUtils.resolveExtension(mimeType);

        Path resolved = uploadBaseDir
                .resolve(filename)      // e.g. uploads/<uuid>.pdf
                .normalize();

        // Security check: UUID filenames can never escape uploadBaseDir,
        // but we keep this as defence-in-depth against future refactors
        if (!resolved.startsWith(uploadBaseDir)) {
            throw new SecurityException("Path traversal detected for documentId: " + documentId);
        }

        return resolved;
    }

    @Override
    public InputStream retrieve(UUID documentId, String mimeType) {
        Path targetPath = buildSafeStoragePath(documentId, mimeType);

        try {
            // Check if the file exists to provide a clearer error message before opening the stream
            if (!Files.exists(targetPath)) {
                log.warn("Attempted to retrieve a non-existent file at: {}", targetPath);
                throw new StorageException("File not found for document ID: " + documentId);
            }

            // Returns the InputStream. The caller (e.g., your Tika logic) is responsible for closing it.
            return Files.newInputStream(targetPath);
        } catch (IOException e) {
            log.error("Error retrieving file at: {}", targetPath, e);
            throw new StorageException("Failed to read file from disk.", e);
        }
    }
}