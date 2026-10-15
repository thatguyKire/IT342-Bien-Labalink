package edu.cit.bien.labalink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** One-time code handed to the web app after Google OAuth2 login. */
@Data
public class OAuth2ExchangeRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 128, message = "Code is too long")
    private String code;
}
