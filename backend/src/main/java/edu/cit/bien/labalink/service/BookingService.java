package edu.cit.bien.labalink.service;

import edu.cit.bien.labalink.config.AppProperties;
import edu.cit.bien.labalink.dto.BookingRequest;
import edu.cit.bien.labalink.dto.BookingResponse;
import edu.cit.bien.labalink.dto.BookingStats;
import edu.cit.bien.labalink.exception.ApiException;
import edu.cit.bien.labalink.model.Booking;
import edu.cit.bien.labalink.model.Machine;
import edu.cit.bien.labalink.model.User;
import edu.cit.bien.labalink.repository.BookingRepository;
import edu.cit.bien.labalink.repository.MachineRepository;
import edu.cit.bien.labalink.repository.UserRepository;
import edu.cit.bien.labalink.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final MachineRepository machineRepository;
    private final EmailService emailService;
    private final WebSocketService webSocketService;
    private final AppProperties properties;

    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings(Booking.BookingStatus status) {
        List<Booking> bookings = status == null
            ? bookingRepository.findAllByOrderByCreatedAtDesc()
            : bookingRepository.findByStatusOrderByCreatedAtDesc(status);
        return bookings.stream().map(BookingResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsForUser(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(BookingResponse::fromEntity)
            .toList();
    }

    /** Owners and attendants only; others get 404 so IDs can't be probed. */
    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long id, UserPrincipal caller) {
        Booking booking = bookingRepository.findById(id)
            .filter(b -> caller.isAttendant()
                || b.getUser().getId().equals(caller.id()))
            .orElseThrow(() -> ApiException.notFound("Booking not found"));
        return BookingResponse.fromEntity(booking);
    }

    /**
     * Starts a session after the customer scans the machine's QR sticker.
     * Machine and wallet rows are locked so two customers cannot grab the
     * same machine and one wallet cannot be spent twice concurrently.
     */
    @Transactional
    public BookingResponse createBooking(
            UserPrincipal caller,
            BookingRequest request) {

        Machine machine = machineRepository
            .findByIdForUpdate(request.getMachineId())
            .orElseThrow(() -> ApiException.notFound("Machine not found"));

        if (!constantTimeEquals(machine.getQrToken(),
                request.getQrToken().trim())) {
            throw ApiException.badRequest(
                "This QR code does not belong to the selected machine");
        }
        if (machine.getStatus() != Machine.MachineStatus.AVAILABLE) {
            throw ApiException.conflict("Machine is not available");
        }

        int duration = resolveDuration(request.getDurationMinutes());
        BigDecimal price = machine.getHourlyRate()
            .multiply(BigDecimal.valueOf(duration))
            .divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP);

        User user = userRepository.findByIdForUpdate(caller.id())
            .orElseThrow(() -> ApiException.unauthorized("User not found"));
        if (user.getWalletBalance().compareTo(price) < 0) {
            throw ApiException.paymentRequired(
                "Insufficient wallet balance. Please top up first.");
        }
        user.setWalletBalance(user.getWalletBalance().subtract(price));
        userRepository.save(user);

        LocalDateTime now = LocalDateTime.now();
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setMachine(machine);
        booking.setStartTime(now);
        booking.setEndTime(now.plusMinutes(duration));
        booking.setDurationMinutes(duration);
        booking.setTotalPrice(price);
        booking.setStatus(Booking.BookingStatus.PENDING);

        machine.setStatus(Machine.MachineStatus.RUNNING);
        machineRepository.save(machine);

        BookingResponse response =
            BookingResponse.fromEntity(bookingRepository.save(booking));
        BigDecimal newBalance = user.getWalletBalance();
        Long machineId = machine.getId();

        AfterCommit.run(() -> {
            webSocketService.broadcastBookingCreated(response);
            webSocketService.broadcastMachineUpdate(
                Map.of("id", machineId, "status", "RUNNING"));
            webSocketService.sendWalletUpdate(caller.id(), newBalance);
        });
        return response;
    }

    /** Attendant workflow: PENDING -> ACTIVE -> COMPLETED, or CANCELLED. */
    @Transactional
    public BookingResponse updateBookingStatus(
            Long id,
            Booking.BookingStatus newStatus) {
        Booking booking = bookingRepository.findByIdForUpdate(id)
            .orElseThrow(() -> ApiException.notFound("Booking not found"));
        return transition(booking, newStatus);
    }

    /** Customers may cancel their own booking until it is activated. */
    @Transactional
    public BookingResponse cancelOwnBooking(Long id, UserPrincipal caller) {
        Booking booking = bookingRepository.findByIdForUpdate(id)
            .filter(b -> b.getUser().getId().equals(caller.id()))
            .orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw ApiException.conflict(
                "Only pending bookings can be cancelled. "
                + "Please ask the attendant.");
        }
        return transition(booking, Booking.BookingStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public BookingStats getStats() {
        return new BookingStats(
            bookingRepository.count(),
            bookingRepository.countByStatus(Booking.BookingStatus.ACTIVE),
            bookingRepository.countByStatus(Booking.BookingStatus.PENDING),
            bookingRepository.sumCompletedRevenue());
    }

    private BookingResponse transition(
            Booking booking,
            Booking.BookingStatus newStatus) {
        Booking.BookingStatus current = booking.getStatus();
        if (!current.canTransitionTo(newStatus)) {
            throw ApiException.conflict(
                "Cannot change a " + current + " booking to " + newStatus);
        }
        booking.setStatus(newStatus);

        // Lock rows *before* reading them (getId() on a lazy proxy does not
        // load it), so we never act on a stale balance or machine status.
        Long userId = booking.getUser().getId();
        Long machineId = booking.getMachine().getId();
        boolean closing = !newStatus.isOpen();
        boolean refunding = newStatus == Booking.BookingStatus.CANCELLED;

        Machine machine = closing
            ? machineRepository.findByIdForUpdate(machineId).orElseThrow()
            : booking.getMachine();
        User user = refunding
            ? userRepository.findByIdForUpdate(userId).orElseThrow()
            : booking.getUser();

        BigDecimal price = booking.getTotalPrice();
        BigDecimal refundedBalance = null;

        if (closing) {
            booking.setEndTime(LocalDateTime.now());
            // Don't override an attendant's OUT_OF_ORDER flag.
            if (machine.getStatus() == Machine.MachineStatus.RUNNING) {
                machine.setStatus(Machine.MachineStatus.AVAILABLE);
                machineRepository.save(machine);
            }
        }

        if (refunding) {
            user.setWalletBalance(user.getWalletBalance().add(price));
            userRepository.save(user);
            refundedBalance = user.getWalletBalance();
        }

        String email = user.getEmail();
        String username = user.getUsername();
        String machineName = machine.getMachineName();
        String machineType = machine.getMachineType().name();
        String startTime = booking.getStartTime().toString();
        String machineStatus = machine.getStatus().name();

        BookingResponse response =
            BookingResponse.fromEntity(bookingRepository.save(booking));
        BigDecimal balanceAfterRefund = refundedBalance;

        AfterCommit.run(() -> {
            webSocketService.broadcastBookingUpdate(response);
            if (closing) {
                webSocketService.broadcastMachineUpdate(
                    Map.of("id", machineId, "status", machineStatus));
            }
            if (balanceAfterRefund != null) {
                webSocketService.sendWalletUpdate(userId, balanceAfterRefund);
            }
            if (newStatus == Booking.BookingStatus.ACTIVE) {
                emailService.sendBookingConfirmationEmail(
                    email, username, machineName, machineType,
                    startTime, price);
            } else if (newStatus == Booking.BookingStatus.COMPLETED) {
                emailService.sendBookingCompletedEmail(
                    email, username, machineName, price);
            }
        });
        return response;
    }

    private int resolveDuration(Integer requested) {
        AppProperties.BookingRules rules = properties.booking();
        int duration = requested == null
            ? rules.defaultDurationMinutes()
            : requested;
        if (duration < rules.minDurationMinutes()
                || duration > rules.maxDurationMinutes()) {
            throw ApiException.badRequest(
                "Duration must be between " + rules.minDurationMinutes()
                + " and " + rules.maxDurationMinutes() + " minutes");
        }
        return duration;
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
            expected.getBytes(StandardCharsets.UTF_8),
            actual.getBytes(StandardCharsets.UTF_8));
    }
}
