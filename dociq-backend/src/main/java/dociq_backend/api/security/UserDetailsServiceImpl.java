package dociq_backend.api.security;

import dociq_backend.domain.model.User;
import dociq_backend.domain.repository.UserRepository; // FIXED: Added missing repository import
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor // FIXED: Automatically injects the UserRepository via constructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository; // FIXED: Declared the required repository dependency

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Find the user by email in your database
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // FIXED: Explicitly use Spring Security's User details builder to avoid naming conflicts with your entity
        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPassword()) // FIXED: Aligned with your entity's 'password' field name
                .authorities("ROLE_" + user.getProfession().name()) // FIXED: Aligned with your entity's 'profession' enum string
                .disabled(!user.getActive()) // Optional helper: links your database 'active' flag directly to Spring Security
                .build();
    }
}