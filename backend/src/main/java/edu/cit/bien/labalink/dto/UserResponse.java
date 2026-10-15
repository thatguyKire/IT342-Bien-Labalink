package edu.cit.bien.labalink.dto;

import edu.cit.bien.labalink.model.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        String role,
        String authProvider,
        BigDecimal walletBalance,
        LocalDateTime createdAt) {

    public static UserResponse fromEntity(User user) {
        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole().name(),
            user.getAuthProvider().name(),
            user.getWalletBalance(),
            user.getCreatedAt());
    }
}
