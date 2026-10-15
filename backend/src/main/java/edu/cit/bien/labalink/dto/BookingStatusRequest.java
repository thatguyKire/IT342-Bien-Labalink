package edu.cit.bien.labalink.dto;

import edu.cit.bien.labalink.model.Booking;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingStatusRequest {

    @NotNull(message = "Status is required")
    private Booking.BookingStatus status;
}
