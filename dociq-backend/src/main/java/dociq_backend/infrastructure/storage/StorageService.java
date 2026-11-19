package dociq_backend.infrastructure.storage;

import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

public interface StorageService {
    /**
     * Stores the file and returns the relative or absolute path to it.
     */


    String store(MultipartFile file, UUID documentId, String mimeType);
    /**
     * Retrieves the file from storage as an InputStream.
     */
    InputStream retrieve(UUID documentId, String mimeType);

    /**
     * Deletes the file from storage.
     */
    String delete(UUID documentId, String detectedMimetype);
}
