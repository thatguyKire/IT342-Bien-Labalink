package edu.cit.bien.labalink.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

/** Wallet top-up amount in PHP; limits are enforced in PaymentService. */
@Data
public class TopUpRequest {

    @NotNull(message = "Amount is required")
    @Digits(integer = 6, fraction = 2,
            message = "Amount may have at most 2 decimals")
    private BigDecimal amount;
}
