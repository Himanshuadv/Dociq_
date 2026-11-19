package dociq_backend.api.controller;

import dociq_backend.application.dto.request.LoginRequest;
import dociq_backend.application.dto.request.RefreshRequest;
import dociq_backend.application.dto.request.RegisterRequest;
import dociq_backend.application.dto.response.AuthResponse;
import dociq_backend.application.service.AuthService;
import jakarta.validation.Valid;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// OpenAPI Imports
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration, login, and session management")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "User Login", description = "Authenticates a user and returns a JWT access token and refresh token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
            @ApiResponse(responseCode = "400", description = "Validation error (e.g., malformed email)"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            @Parameter(description = "The client's browser or device identifier")
            @RequestHeader(value = "User-Agent", defaultValue = "unknown"
    ) String userAgent){



        return ResponseEntity.ok(authService.login(request, userAgent));
    }

    @PostMapping("/register")
    @Operation(summary = "User Registration", description = "Creates a new user account in Dociq.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully created"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid request payload"),
            @ApiResponse(responseCode = "409", description = "Conflict - Email already in use")
    })
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Token", description = "Generates a new JWT access token using a valid refresh token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "New tokens generated successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid, expired, or revoked refresh token")
    })
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req));
    }

    @PostMapping("/logout")
    @Operation(summary = "User Logout", description = "Invalidates the provided refresh token, ending the user's session.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully logged out (No Content)"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest req) {
        authService.logout(req.refreshToken());
        return ResponseEntity.noContent().build();
    }

//    @PostMapping("/logout-all")
//    public ResponseEntity<Void> logoutAll(Authentication auth) {
//        // get user ID from the authenticated principal
//        String email = auth.getName();
//        // You would look up userId from email here or store UUID in JWT claims
//        // For now, delegate to service using email
//        // (add findByEmail or store userId in JWT claims for direct access)
//        return ResponseEntity.noContent().build();
//    }

//    @GetMapping("/me")
//    public ResponseEntity<Map<String, Object>> me(Authentication auth) {
//        return ResponseEntity.ok(Map.of("email", auth.getName()));
//    }
}