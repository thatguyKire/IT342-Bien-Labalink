package edu.cit.bien.labalink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Customer booking request. The user comes from the JWT and the price is
 * computed by the server, so neither can be supplied (or forged) here.
 */
@Data
public class BookingRequest {

    @NotNull(message = "Machine ID is required")
    @Positive(message = "Machine ID is invalid")
    private Long machineId;

    @NotBlank(message = "Scan the machine QR code to start")
    @Size(max = 64, message = "QR code is invalid")
    private String qrToken;

    // Optional; the server default applies when null.
    private Integer durationMinutes;
}
