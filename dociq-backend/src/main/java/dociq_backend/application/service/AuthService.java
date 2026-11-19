package dociq_backend.application.service; // Fix: Stripped 'main.java' prefix

import dociq_backend.api.exception.DuplicateResourceException;
import dociq_backend.api.security.JwtService;
import dociq_backend.application.dto.request.LoginRequest;
import dociq_backend.application.dto.request.RefreshRequest;
import dociq_backend.application.dto.request.RegisterRequest;
import dociq_backend.application.dto.response.AuthResponse;
import dociq_backend.domain.model.RefreshToken;
import dociq_backend.domain.model.User;
import dociq_backend.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service // Tells Spring Boot to manage this class as a Service bean
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    // Spring automatically injects these dependencies via constructor
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already in use");
        }
        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.profession()
        );

        userRepository.save(user);

       // String token = jwtService.generateToken(user);

        return buildAuthResponse(user, "unknown device");
    }

    public AuthResponse login(LoginRequest request, String userAgent) {
        // 1. Corrected: 'User' instead of 'Optional<User>'
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new dociq_backend.api.exception.UnauthorizedException("Invalid credentials"));

        if (!user.getActive())
            throw new dociq_backend.api.exception.UnauthorizedException("Account is disabled");

        if (!passwordEncoder.matches(
                request.password(),
                user.getPassword())) {

            throw new dociq_backend.api.exception.UnauthorizedException("Invalid credentials: Wrong Password");
        }

      //  String token = jwtService.generateToken(user);

        return buildAuthResponse(user, userAgent);
    }



    public AuthResponse refresh(RefreshRequest request) {
        // validate old refresh token, get user, delete old token (rotation)
        User old = refreshTokenService.validateAndRotate(request.refreshToken());

        return buildAuthResponse(old, "UNKNOWn");
    }


    public void logout(String rawRefreshToken) {
        try {
            User token = refreshTokenService.validateAndRotate(rawRefreshToken);
            // validateAndRotate already deleted it, logout complete
        } catch (dociq_backend.api.exception.UnauthorizedException ignored) {
            // token already invalid — that's fine for logout
        }
    }

    public void logoutAll(UUID userId) {
        refreshTokenService.revokeAllForUser(userId);
    }

    // ------------------------------------------------------------------ helper------------
    private AuthResponse buildAuthResponse(User user, String deviceInfo) {
        String accessToken = jwtService.generateToken(
                user.getEmail(), Map.of("profession", user.getProfession())
        );
        String refreshToken = refreshTokenService.createRefreshToken(user, deviceInfo);
        Long expiresIn = 15L * 60;
        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getProfession(),
                accessToken,
                refreshToken,
                "Bearer",
                expiresIn,    // 15 minutes in seconds
                "Registration successful"
        );

    }
}