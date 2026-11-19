package dociq_backend.domain.model;

import dociq_backend.domain.enums.Profession;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.sql.ConnectionBuilder;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class) // 1. CRITICAL: Tells JPA to watch this entity for creation/updates
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;


    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;
    @Column(nullable = false)
    // This is for the user's status => we/user can deactivate the account softly
    private Boolean active = true;

    // 2. AUDITING FIELDS ADDED HERE
    @CreatedDate
    @Column(nullable = false, updatable = false) // updatable = false ensures it never changes after creation
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50) // FIXED: Matches VARCHAR(50) in V1 script
    private Profession profession = Profession.PERSON;

    // Your AuthService is trying to use this to register new users!
    public User(String email, String password, Profession profession) {
        this.email = email;
        this.password = password;
        this.profession = profession == null ? Profession.PERSON : profession;
    }



}