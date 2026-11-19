package dociq_backend.application.dto.request;


import dociq_backend.domain.enums.Profession;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Invalid email format")
        String email,


        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8,
                message = "Password must be at least 8 characters")
        String password,

        Profession profession

) {

    public Profession professionOrDefault() {
        return profession == null
                ? Profession.PERSON
                : profession;
    }
}