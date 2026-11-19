package dociq_backend.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    // Reusable ObjectMapper — thread-safe, so safe as a field
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules(); // registers JavaTimeModule for LocalDateTime

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Step 1: No Authorization header — not our problem, pass along
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 2: Extract raw token string
        String token = authHeader.substring(7);

        // Step 3: Parse + validate — catch JWT exceptions directly here.
        // GlobalExceptionHandler won't see these — filters run outside the DispatcherServlet.
        try {
            String email = jwtService.extractSubject(token);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

        } catch (ExpiredJwtException e) {
            log.warn("JWT expired for request [{}]: {}", request.getRequestURI(), e.getMessage());
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Token has expired. Please log in again.");
            return; // ← stop the filter chain, response is already written

        } catch (SignatureException e) {
            log.warn("Invalid JWT signature for request [{}]: {}", request.getRequestURI(), e.getMessage());
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid token signature.");
            return;

        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT for request [{}]: {}", request.getRequestURI(), e.getMessage());
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Malformed token.");
            return;

        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT for request [{}]: {}", request.getRequestURI(), e.getMessage());
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Unsupported token format.");
            return;

        } catch (JwtException e) {
            // Catch-all for any other JJWT library exceptions
            log.warn("JWT error for request [{}]: {}", request.getRequestURI(), e.getMessage());
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid access token.");
            return;
        }

        // Step 4: Token valid (or no token) — continue
        filterChain.doFilter(request, response);
    }

    // Writes a consistent JSON error body directly to the response
    private void writeErrorResponse(HttpServletResponse response,
                                    HttpStatus status,
                                    String message) throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}