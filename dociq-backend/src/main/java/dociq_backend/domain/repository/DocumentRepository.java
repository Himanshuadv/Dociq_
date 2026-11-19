package dociq_backend.domain.repository;

import dociq_backend.domain.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    // All documents for a user (for listing their uploads)
    List<Document> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Check for duplicate stored filename (defensive)
    boolean existsByStoredFilename(String storedFilename);

    // Find by user + document id together (prevents users accessing other users' docs)
    Optional<Document> findByIdAndUserId(UUID id, UUID userId);

    // Count documents per user (for quota enforcement later)
    long countByUserId(UUID userId);
}