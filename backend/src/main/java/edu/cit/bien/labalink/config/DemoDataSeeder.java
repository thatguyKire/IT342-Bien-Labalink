package edu.cit.bien.labalink.config;

import edu.cit.bien.labalink.dto.MachineRequest;
import edu.cit.bien.labalink.model.Booking;
import edu.cit.bien.labalink.model.Booking.BookingStatus;
import edu.cit.bien.labalink.model.Machine;
import edu.cit.bien.labalink.model.Machine.MachineStatus;
import edu.cit.bien.labalink.model.Machine.MachineType;
import edu.cit.bien.labalink.model.Payment;
import edu.cit.bien.labalink.model.Payment.PaymentStatus;
import edu.cit.bien.labalink.model.User;
import edu.cit.bien.labalink.repository.BookingRepository;
import edu.cit.bien.labalink.repository.MachineRepository;
import edu.cit.bien.labalink.repository.PaymentRepository;
import edu.cit.bien.labalink.repository.UserRepository;
import edu.cit.bien.labalink.service.BookingService;
import edu.cit.bien.labalink.service.MachineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static edu.cit.bien.labalink.model.Booking.BookingStatus.ACTIVE;
import static edu.cit.bien.labalink.model.Booking.BookingStatus.CANCELLED;
import static edu.cit.bien.labalink.model.Booking.BookingStatus.COMPLETED;
import static edu.cit.bien.labalink.model.Booking.BookingStatus.PENDING;
import static edu.cit.bien.labalink.model.Machine.MachineType.DRYER;
import static edu.cit.bien.labalink.model.Machine.MachineType.WASHER;
import static edu.cit.bien.labalink.model.Payment.PaymentStatus.FAILED;
import static edu.cit.bien.labalink.model.Payment.PaymentStatus.SUCCESS;

/**
 * Fills an empty database with sample customers, machines, bookings and
 * top-ups so every screen has something to show in demos and screenshots.
 * Runs only with DEMO_DATA=true, and never touches a database that already
 * has machines.
 *
 * <p>Machines go through {@link MachineService} so they get real QR
 * secrets. Bookings and top-ups are history, which the live services
 * cannot create (they always start "now", charge cards and send emails),
 * so they are written directly. Wallet balances are derived from that
 * history, so they always add up.
 */
@Slf4j
@Component
@ConditionalOnProperty(
    prefix = "app.demo", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    /** The customer to sign in as on the Android app. */
    public static final String PRIMARY_CUSTOMER_EMAIL = "maria@labalink.test";

    // Reserved .test domain: these addresses can never receive real mail.
    private static final String DEMO_EMAIL_DOMAIN = "@labalink.test";

    private final AppProperties properties;
    private final UserRepository userRepository;
    private final MachineRepository machineRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final MachineService machineService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (machineRepository.count() > 0
                || userRepository.existsByEmail(PRIMARY_CUSTOMER_EMAIL)) {
            log.info("Demo data skipped: the database already has data");
            return;
        }
        LocalDateTime now = LocalDateTime.now();

        User maria = customer("Maria Santos", PRIMARY_CUSTOMER_EMAIL, now);
        User juan = customer("Juan Reyes", "juan" + DEMO_EMAIL_DOMAIN, now);
        User ana = customer("Ana Cruz", "ana" + DEMO_EMAIL_DOMAIN, now);

        Machine washer1 = machine("Washer 1", WASHER, "60");
        Machine washer2 = machine("Washer 2", WASHER, "60");
        Machine washer3 = machine("Washer 3", WASHER, "60");
        Machine washer4 = machine("Washer 4 (Large)", WASHER, "80");
        Machine dryer1 = machine("Dryer 1", DRYER, "50");
        Machine dryer2 = machine("Dryer 2", DRYER, "50");
        Machine dryer3 = machine("Dryer 3", DRYER, "50");
        Machine dryer4 = machine("Dryer 4", DRYER, "50");
        dryer3.setStatus(MachineStatus.OUT_OF_ORDER);

        // The past nine days, oldest first.
        topUp(maria, "500", daysAgo(now, 9, 8, 40), SUCCESS);
        session(maria, washer1, 60, daysAgo(now, 8, 9, 0), COMPLETED);
        session(maria, dryer1, 45, daysAgo(now, 8, 10, 5), COMPLETED);
        topUp(juan, "300", daysAgo(now, 8, 13, 20), SUCCESS);
        session(juan, washer2, 60, daysAgo(now, 7, 11, 0), COMPLETED);
        topUp(ana, "200", daysAgo(now, 7, 12, 10), SUCCESS);
        session(ana, washer3, 60, daysAgo(now, 6, 13, 0), COMPLETED);
        session(maria, washer3, 60, daysAgo(now, 5, 14, 0), COMPLETED);
        topUp(maria, "300", daysAgo(now, 4, 9, 30), SUCCESS);
        session(juan, dryer4, 60, daysAgo(now, 4, 16, 0), COMPLETED);
        session(maria, washer2, 30, daysAgo(now, 3, 18, 30), CANCELLED);
        topUp(juan, "200", daysAgo(now, 3, 19, 0), SUCCESS);
        session(juan, washer1, 45, daysAgo(now, 2, 10, 0), COMPLETED);
        topUp(ana, "150", daysAgo(now, 2, 15, 10), FAILED);
        session(ana, dryer1, 30, daysAgo(now, 2, 15, 30), CANCELLED);
        session(maria, washer4, 90, daysAgo(now, 1, 8, 15), COMPLETED);

        // Happening right now: these machines show as RUNNING.
        session(juan, washer2, 60, now.minusMinutes(35), ACTIVE);
        session(maria, dryer2, 60, now.minusMinutes(20), ACTIVE);
        session(ana, washer4, 60, now.minusMinutes(3), PENDING);

        log.info("Demo data loaded. Sign in to the app as {} with "
            + "DEMO_PASSWORD.", PRIMARY_CUSTOMER_EMAIL);
    }

    private User customer(String name, String email, LocalDateTime now) {
        User user = new User();
        user.setUsername(name);
        user.setEmail(email);
        user.setPasswordHash(
            passwordEncoder.encode(properties.demo().password()));
        user.setRole(User.Role.CUSTOMER);
        user.setAuthProvider(User.AuthProvider.LOCAL);
        user.setCreatedAt(now.minusDays(10));
        return userRepository.save(user);
    }

    private Machine machine(String name, MachineType type, String rate) {
        MachineRequest request = new MachineRequest();
        request.setMachineName(name);
        request.setMachineType(type);
        request.setHourlyRate(new BigDecimal(rate));
        Long id = machineService.createMachine(request).getId();
        return machineRepository.findById(id).orElseThrow();
    }

    private void topUp(
            User user,
            String amount,
            LocalDateTime at,
            PaymentStatus status) {
        Payment payment = new Payment();
        payment.setUser(user);
        payment.setAmount(new BigDecimal(amount));
        payment.setStatus(status);
        payment.setCreatedAt(at);
        if (status == SUCCESS) {
            payment.setPaidAt(at);
            adjustWallet(user, payment.getAmount());
        }
        Payment saved = paymentRepository.save(payment);
        saved.setProviderReference("LL-" + saved.getId() + "-DEMO");
    }

    private void session(
            User user,
            Machine machine,
            int minutes,
            LocalDateTime start,
            BookingStatus status) {
        BigDecimal price =
            BookingService.priceFor(machine.getHourlyRate(), minutes);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setMachine(machine);
        booking.setDurationMinutes(minutes);
        booking.setTotalPrice(price);
        booking.setStatus(status);
        booking.setStartTime(start);
        booking.setCreatedAt(start);
        booking.setEndTime(status == CANCELLED
            ? start.plusMinutes(5)
            : start.plusMinutes(minutes));

        // Same money rules as BookingService: charged at start, refunded
        // on cancel, so a cancelled session costs nothing.
        if (status != CANCELLED) {
            adjustWallet(user, price.negate());
        }
        if (status.isOpen()) {
            machine.setStatus(MachineStatus.RUNNING);
        }
        bookingRepository.save(booking);
    }

    private static void adjustWallet(User user, BigDecimal delta) {
        BigDecimal balance = user.getWalletBalance().add(delta);
        if (balance.signum() < 0) {
            throw new IllegalStateException(
                "Demo data would overdraw " + user.getEmail());
        }
        user.setWalletBalance(balance);
    }

    private static LocalDateTime daysAgo(
            LocalDateTime now,
            int days,
            int hour,
            int minute) {
        return now.toLocalDate().minusDays(days).atTime(hour, minute);
    }
}
