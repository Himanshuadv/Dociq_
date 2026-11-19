// CORRECT:
package dociq_backend.domain.repository;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<dociq_backend.domain.model.User, Long> {
    // This automatically generates the SQL to find a user by their username!
    Optional<dociq_backend.domain.model.User> findByEmail(String username);

    boolean existsByEmail(@NotBlank(message = "Email cannot be blank") @Email(message = "Invalid email format") String email);

    //  Optional<Object> findByEmail(@NotBlank(message = "Email cannot be blank") @Email(message = "Invalid email format") String email);
}