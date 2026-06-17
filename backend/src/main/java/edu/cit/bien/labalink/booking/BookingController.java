package edu.cit.bien.labalink.booking;

import edu.cit.bien.labalink.auth.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<List<BookingResponse>>
            getAllBookings(
                @RequestParam(required = false)
                String status) {
        if (status != null && !status.isEmpty()) {
            try {
                Booking.BookingStatus bookingStatus =
                    Booking.BookingStatus
                        .valueOf(status.toUpperCase());
                return ResponseEntity.ok(
                    bookingService
                        .getBookingsByStatus(
                            bookingStatus));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.ok(
                    bookingService.getAllBookings());
            }
        }
        return ResponseEntity.ok(
            bookingService.getAllBookings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBookingById(
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(
                bookingService.getBookingById(id));
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(
                    false, e.getMessage()));
        }
    }



    @PostMapping
    public ResponseEntity<?> createBooking(
            @Valid @RequestBody
            BookingRequest request) {
        try {
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(bookingService
                    .createBooking(request));
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(
                    false, e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            Booking.BookingStatus status =
                Booking.BookingStatus
                    .valueOf(body.get("status")
                        .toUpperCase());
            return ResponseEntity.ok(
                bookingService
                    .updateBookingStatus(id, status));
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(
                    false, e.getMessage()));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(Map.of(
            "totalBookings",
                bookingService.getTotalBookings(),
            "activeBookings",
                bookingService.getActiveBookings(),
            "totalRevenue",
                bookingService.getTotalRevenue()
        ));
    }


    // 🟢 ADD THIS NEW ENDPOINT FOR QR SCANNING
    @PostMapping("/qr")
    public ResponseEntity<?> activateMachineByQr(@RequestBody Map<String, Object> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            String qrToken = payload.get("qrToken").toString();

            // NOTE: You must have a method in your BookingService to handle this!
            // It should look up the machine by the qrToken, calculate the time/price, 
            // and save the booking.
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(bookingService.activateByQr(userId, qrToken)); 
                
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(false, e.getMessage()));
        }
    }
}