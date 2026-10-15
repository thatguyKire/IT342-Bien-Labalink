package edu.cit.bien.labalink.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.cit.bien.labalink.model.Payment;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {
    private Long id;
    private String userEmail;
    private String username;
    private BigDecimal amount;
    private String currency;
    private String providerReference;
    private Payment.PaymentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    // Hosted Stripe Checkout page; only set right after creation.
    private String checkoutUrl;

    public static PaymentResponse fromEntity(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setUserEmail(payment.getUser().getEmail());
        response.setUsername(payment.getUser().getUsername());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setProviderReference(
            payment.getProviderReference());
        response.setStatus(payment.getStatus());
        response.setCreatedAt(payment.getCreatedAt());
        response.setPaidAt(payment.getPaidAt());
        return response;
    }
}
