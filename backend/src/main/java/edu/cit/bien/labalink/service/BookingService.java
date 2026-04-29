package edu.cit.bien.labalink.service;

import edu.cit.bien.labalink.dto.BookingRequest;
import edu.cit.bien.labalink.dto.BookingResponse;
import edu.cit.bien.labalink.model.Booking;
import edu.cit.bien.labalink.model.Machine;
import edu.cit.bien.labalink.model.User;
import edu.cit.bien.labalink.repository.BookingRepository;
import edu.cit.bien.labalink.repository.MachineRepository;
import edu.cit.bien.labalink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository 
        bookingRepository;
    private final UserRepository userRepository;
    private final MachineRepository 
        machineRepository;

    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll()
            .stream()
            .map(BookingResponse::fromEntity)
            .collect(Collectors.toList());
    }

    public List<BookingResponse> getBookingsByStatus(
            Booking.BookingStatus status) {
        return bookingRepository
            .findByStatus(status)
            .stream()
            .map(BookingResponse::fromEntity)
            .collect(Collectors.toList());
    }

    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository
            .findById(id)
            .orElseThrow(() ->
                new RuntimeException(
                    "Booking not found"));
        return BookingResponse.fromEntity(booking);
    }

    public BookingResponse createBooking(
            BookingRequest request) {

        User user = userRepository
            .findById(request.getUserId())
            .orElseThrow(() ->
                new RuntimeException(
                    "User not found"));

        Machine machine = machineRepository
            .findById(request.getMachineId())
            .orElseThrow(() ->
                new RuntimeException(
                    "Machine not found"));

        if (machine.getStatus() != 
                Machine.MachineStatus.AVAILABLE) {
            throw new RuntimeException(
                "Machine is not available");
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setMachine(machine);
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setTotalPrice(request.getTotalPrice());
        booking.setStatus(Booking.BookingStatus.PENDING);

        // Update machine status to RUNNING
        machine.setStatus(
            Machine.MachineStatus.RUNNING);
        machineRepository.save(machine);

        return BookingResponse.fromEntity(
            bookingRepository.save(booking));
    }

    public BookingResponse updateBookingStatus(
            Long id, 
            Booking.BookingStatus status) {

        Booking booking = bookingRepository
            .findById(id)
            .orElseThrow(() ->
                new RuntimeException(
                    "Booking not found"));

        booking.setStatus(status);

        // If completed or cancelled
        // set machine back to AVAILABLE
        if (status == 
                Booking.BookingStatus.COMPLETED ||
            status == 
                Booking.BookingStatus.CANCELLED) {

            booking.setEndTime(LocalDateTime.now());

            Machine machine = booking.getMachine();
            machine.setStatus(
                Machine.MachineStatus.AVAILABLE);
            machineRepository.save(machine);
        }

        return BookingResponse.fromEntity(
            bookingRepository.save(booking));
    }

    // Stats for dashboard cards
    public long getTotalBookings() {
        return bookingRepository.count();
    }

    public long getActiveBookings() {
        return bookingRepository
            .findByStatus(
                Booking.BookingStatus.ACTIVE)
            .size();
    }

    public Double getTotalRevenue() {
        return bookingRepository.findAll()
            .stream()
            .filter(b -> b.getStatus() ==
                Booking.BookingStatus.COMPLETED)
            .mapToDouble(Booking::getTotalPrice)
            .sum();
    }
}