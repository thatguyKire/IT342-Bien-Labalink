package edu.cit.bien.labalink.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String role;
    private String email;
    private String username;
    private Long userId;
}
