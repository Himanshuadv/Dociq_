package dociq_backend.application.dto.response;

import dociq_backend.domain.enums.Profession;

import java.util.UUID;

public record AuthResponse(
        UUID id,
        String email,
        Profession profession,
        String accessToken,  // Added: The short-lived JWT pass
        String refreshToken, // Added: The long-lived key to get new JWTs
        String tokenType,    // Added: Usually "Bearer"
        Long expiresIn,      // Added: Expiration duration in milliseconds/seconds
        String message
) {}