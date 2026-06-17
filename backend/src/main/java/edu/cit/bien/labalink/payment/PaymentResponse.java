package edu.cit.bien.labalink.payment;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {
    private Long id;
    private String userEmail;
    private String username;
    private Double amount;
    private String providerReference;
    private String clientSecret;
    private Payment.PaymentStatus status;
    private LocalDateTime createdAt;

    public static PaymentResponse fromEntity(
            Payment payment) {
        PaymentResponse response = 
            new PaymentResponse();
        response.setId(payment.getId());
        response.setUserEmail(
            payment.getUser().getEmail());
        response.setUsername(
            payment.getUser().getUsername());
        response.setAmount(payment.getAmount());
        response.setProviderReference(
            payment.getProviderReference());
        response.setStatus(payment.getStatus());
        response.setCreatedAt(payment.getCreatedAt());
        return response;
    }
}