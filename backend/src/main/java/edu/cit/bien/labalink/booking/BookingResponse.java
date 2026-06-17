package edu.cit.bien.labalink.booking;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BookingResponse {
    private Long id;
    private String userEmail;
    private String username;
    private Long machineId;
    private String machineName;
    private String machineType;
    private Booking.BookingStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double totalPrice;
    private LocalDateTime createdAt;

    public static BookingResponse fromEntity(
            Booking booking) {
        BookingResponse response = 
            new BookingResponse();
        response.setId(booking.getId());
        response.setUserEmail(
            booking.getUser().getEmail());
        response.setUsername(
            booking.getUser().getUsername());
        response.setMachineId(
            booking.getMachine().getId());
        response.setMachineName(
            booking.getMachine().getMachineName());
        response.setMachineType(
            booking.getMachine()
                .getMachineType().name());
        response.setStatus(booking.getStatus());
        response.setStartTime(booking.getStartTime());
        response.setEndTime(booking.getEndTime());
        response.setTotalPrice(booking.getTotalPrice());
        response.setCreatedAt(booking.getCreatedAt());
        return response;
    }
}