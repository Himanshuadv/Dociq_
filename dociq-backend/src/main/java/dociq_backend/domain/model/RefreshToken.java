package dociq_backend.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

// domain/repository/RefreshTokenRepository.java
// domain/model/RefreshToken.java
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 64) // FIXED: Matches VARCHAR(64) in V2 script
    private String tokenHash;           // SHA-256 hash of the actual token

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private String deviceInfo;          // optional: "Chrome/Mac"

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }
}