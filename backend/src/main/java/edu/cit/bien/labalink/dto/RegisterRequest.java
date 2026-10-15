package edu.cit.bien.labalink.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Self-registration payload. There is deliberately no "role" field:
 * public sign-up always creates a CUSTOMER. Attendants are provisioned
 * by another attendant (POST /api/users/attendants) or the bootstrap config.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50,
          message = "Username must be 3-50 characters")
    @Pattern(regexp = "^[\\p{L}\\p{N} ._-]+$",
             message = "Username may only contain letters, numbers, "
                 + "spaces, dots, underscores and hyphens")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 254, message = "Email is too long")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72,
          message = "Password must be 8-72 characters")
    @Pattern(regexp = "^(?=.*\\p{L})(?=.*\\p{N}).+$",
             message = "Password must contain at least one letter "
                 + "and one number")
    private String password;
}
