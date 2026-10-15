package edu.cit.bien.labalink.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 254, message = "Email is too long")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(max = 128, message = "Password is too long")
    private String password;
}
