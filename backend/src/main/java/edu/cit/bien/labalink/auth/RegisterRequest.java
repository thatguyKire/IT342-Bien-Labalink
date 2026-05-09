package edu.cit.bien.labalink.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, 
          message = "Password must be at least 8 characters")
    private String password;

    // Optional — defaults to CUSTOMER
    // Mobile always sends CUSTOMER
    private User.Role role = User.Role.CUSTOMER;
}