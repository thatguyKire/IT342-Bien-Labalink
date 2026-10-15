package edu.cit.bien.labalink.controller;

import edu.cit.bien.labalink.dto.BookingRequest;
import edu.cit.bien.labalink.dto.BookingResponse;
import edu.cit.bien.labalink.dto.BookingStats;
import edu.cit.bien.labalink.dto.BookingStatusRequest;
import edu.cit.bien.labalink.model.Booking;
import edu.cit.bien.labalink.security.UserPrincipal;
import edu.cit.bien.labalink.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /** Attendant view of every booking, optionally filtered by status. */
    @GetMapping
    @PreAuthorize("hasRole('ATTENDANT')")
    public List<BookingResponse> getAllBookings(
            @RequestParam(required = false) Booking.BookingStatus status) {
        return bookingService.getAllBookings(status);
    }

    /** The caller's own bookings. */
    @GetMapping("/me")
    public List<BookingResponse> getMyBookings(
            @AuthenticationPrincipal UserPrincipal principal) {
        return bookingService.getBookingsForUser(principal.id());
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ATTENDANT')")
    public BookingStats getStats() {
        return bookingService.getStats();
    }

    @GetMapping("/{id}")
    public BookingResponse getBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return bookingService.getBooking(id, principal);
    }

    /** Customer scans a machine's QR code to start (and pay for) a session. */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return bookingService.createBooking(principal, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ATTENDANT')")
    public BookingResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody BookingStatusRequest request) {
        return bookingService.updateBookingStatus(id, request.getStatus());
    }

    /** Customer cancels their own PENDING booking (wallet is refunded). */
    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return bookingService.cancelOwnBooking(id, principal);
    }
}
