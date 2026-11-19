package dociq_backend.application.service;

import dociq_backend.domain.model.RefreshToken;
import dociq_backend.domain.model.User;
import dociq_backend.domain.repository.RefreshTokenRepository;
import jakarta.persistence.Entity;
import jakarta.transaction.Transactional;
import lombok.*;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

// application/service/RefreshTokenService.java
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${dociq.jwt.refresh-token-expiry-days}")
    private long expiryDays;

    // ------------------------------------------------------------------ create
    @Transactional
    public String createRefreshToken(User user, String deviceInfo) {
        // Generate a cryptographically secure random token
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);
        System.out.println("DEBUG: Incoming Raw Token: " + rawToken);
        System.out.println("DEBUG: Computed Hash: " + tokenHash);


        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(Instant.now().plus(expiryDays, ChronoUnit.DAYS))
                .createdAt(Instant.now())
                .deviceInfo(deviceInfo)
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken; // return the PLAIN token to send to client (never stored plain)
    }

    // ---------------------------------------------------------------- validate
    @Transactional
    public User validateAndRotate(String rawToken) { // Changed return type to User
        String tokenHash = hashToken(rawToken);

        // DEBUG: Print the hash to your console
        System.out.println("DEBUG: Incoming Raw Token: " + rawToken);
        System.out.println("DEBUG: Computed Hash: " + tokenHash);

        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    System.out.println("DEBUG: No token found for hash: " + tokenHash);
                    return new dociq_backend.api.exception.UnauthorizedException("Invalid refresh token");
                });

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            // Force initialization of the lazy proxy BEFORE the session closes

            refreshTokenRepository.delete(stored);
            throw new dociq_backend.api.exception.UnauthorizedException("Refresh token expired. Please log in again.");
        }

        // 1. Extract the User safely BEFORE deleting the token
        Hibernate.initialize(stored.getUser());
        User verifiedUser = stored.getUser();

        // 2. Token rotation: delete old token immediately so it can never be used again
        refreshTokenRepository.delete(stored);

        // 3. Return the clean User object to the caller
        return verifiedUser;
    }

    // ------------------------------------------------------------------ logout
    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.deleteAllByUserId(userId);
    }

    // -------------------------------------------------------- scheduled cleanup
    @Scheduled(cron = "0 0 2 * * *")  // every night at 2am
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteAllExpired(Instant.now());
    }

    // ------------------------------------------------------------ private utils
    private String generateSecureToken() {
        byte[] bytes = new byte[32]; // 256 bits
        new java.security.SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}