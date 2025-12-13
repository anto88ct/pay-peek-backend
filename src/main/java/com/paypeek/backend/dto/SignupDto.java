package com.paypeek.backend.dto;

import com.paypeek.backend.dto.validators.PasswordMatches;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@PasswordMatches
public class SignupDto {

    @NotBlank(message = "Nome obbligatorio")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ '’-]+$", message = "Il nome contiene caratteri non validi")
    private String firstName;

    @NotBlank(message = "Cognome obbligatorio")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ '’-]+$", message = "Il cognome contiene caratteri non validi")
    private String lastName;

    @Email(message = "Email non valida")
    @NotBlank(message = "Email obbligatoria")
    private String email;

    @NotBlank(message = "Password obbligatoria")
    @Size(min = 8, message = "Password deve essere almeno 8 caratteri")
    private String password;

    @NotBlank(message = "Conferma password obbligatoria")
    @Size(min = 8, message = "Password deve essere almeno 8 caratteri")
    private String confirmPassword;

    private String job;
    private String nationality;
    private String city;
    private String country;
}
