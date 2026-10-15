package edu.cit.bien.labalink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Google ID token obtained by the Android app via requestIdToken(). */
@Data
public class GoogleTokenRequest {

    @NotBlank(message = "ID token is required")
    @Size(max = 4096, message = "ID token is too long")
    private String idToken;
}
