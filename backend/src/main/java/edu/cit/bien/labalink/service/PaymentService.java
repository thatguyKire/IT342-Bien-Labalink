package edu.cit.bien.labalink.service;

import edu.cit.bien.labalink.config.AppProperties;
import edu.cit.bien.labalink.dto.PaymentResponse;
import edu.cit.bien.labalink.exception.ApiException;
import edu.cit.bien.labalink.model.Payment;
import edu.cit.bien.labalink.model.User;
import edu.cit.bien.labalink.repository.PaymentRepository;
import edu.cit.bien.labalink.repository.UserRepository;
import edu.cit.bien.labalink.security.UserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Wallet top-ups through Stripe Checkout.
 *
 * Money is credited only after the server itself has confirmed with Stripe
 * (webhook or explicit sync) that the session is paid, for exactly the
 * amount and currency recorded on our side. Crediting is idempotent:
 * the payment row is locked and only a PENDING payment can succeed, so
 * replaying a webhook or calling sync twice can never double the money.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Set<String> HANDLED_EVENTS = Set.of(
        "checkout.session.completed",
        "checkout.session.async_payment_succeeded",
        "checkout.session.async_payment_failed",
        "checkout.session.expired");

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PaymentGateway gateway;
    private final EmailService emailService;
    private final WebSocketService webSocketService;
    private final AppProperties properties;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public PaymentResponse createCheckout(
            UserPrincipal caller,
            BigDecimal rawAmount) {
        if (!gateway.isConfigured()) {
            throw ApiException.unavailable(
                "Online payments are not configured on the server");
        }

        AppProperties.Wallet limits = properties.wallet();
        BigDecimal amount = rawAmount.setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(limits.minTopUp()) < 0
                || amount.compareTo(limits.maxTopUp()) > 0) {
            throw ApiException.badRequest(
                "Top-up must be between PHP " + limits.minTopUp()
                + " and PHP " + limits.maxTopUp());
        }

        User user = userRepository.findById(caller.id())
            .orElseThrow(() -> ApiException.unauthorized("User not found"));
        if (user.getWalletBalance().add(amount)
                .compareTo(limits.maxBalance()) > 0) {
            throw ApiException.badRequest(
                "Wallet balance cannot exceed PHP " + limits.maxBalance());
        }

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setAmount(amount);
        payment.setCurrency(properties.stripe().currency());
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment = paymentRepository.save(payment);

        PaymentGateway.CheckoutSession session;
        try {
            session = gateway.createCheckoutSession(
                payment.getId(),
                user.getId(),
                user.getEmail(),
                toMinorUnits(amount),
                payment.getCurrency(),
                "labalink-topup-" + payment.getId());
        } catch (PaymentGateway.PaymentProviderException e) {
            log.error("Checkout creation failed for payment {}",
                payment.getId(), e);
            throw ApiException.unavailable(
                "Payment provider is unavailable. Please try again.");
        }

        payment.setStripeCheckoutSessionId(session.id());
        payment.setProviderReference(reference(payment.getId(), session.id()));
        paymentRepository.save(payment);

        PaymentResponse response = PaymentResponse.fromEntity(payment);
        response.setCheckoutUrl(session.url());
        return response;
    }

    /**
     * Asks Stripe for the current state of the caller's payment and applies
     * it. Lets the app confirm a top-up even when webhooks can't reach a
     * local development server.
     */
    @Transactional
    public PaymentResponse syncPayment(Long paymentId, UserPrincipal caller) {
        Payment payment = paymentRepository.findById(paymentId)
            .filter(p -> caller.isAttendant()
                || p.getUser().getId().equals(caller.id()))
            .orElseThrow(() -> ApiException.notFound("Payment not found"));

        if (payment.getStatus() == Payment.PaymentStatus.PENDING
                && payment.getStripeCheckoutSessionId() != null
                && gateway.isConfigured()) {
            try {
                applySession(paymentId, gateway.retrieveCheckoutSession(
                    payment.getStripeCheckoutSessionId()));
            } catch (PaymentGateway.PaymentProviderException e) {
                log.error("Payment sync failed for payment {}", paymentId, e);
                throw ApiException.unavailable(
                    "Payment provider is unavailable. Please try again.");
            }
        }
        return PaymentResponse.fromEntity(
            paymentRepository.findById(paymentId).orElseThrow());
    }

    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        String secret = properties.stripe().webhookSecret();
        if (secret == null || secret.isBlank()) {
            throw ApiException.unavailable("Webhooks are not configured");
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw ApiException.badRequest("Missing signature");
        }

        PaymentGateway.WebhookEvent event;
        try {
            event = gateway.parseWebhook(payload, signatureHeader);
        } catch (IllegalArgumentException e) {
            log.warn("Rejected webhook with invalid signature");
            throw ApiException.badRequest("Invalid signature");
        }

        if (event == null || !HANDLED_EVENTS.contains(event.type())) {
            return;
        }
        paymentRepository
            .findByStripeCheckoutSessionId(event.session().id())
            .ifPresentOrElse(
                payment -> applySession(payment.getId(), event.session()),
                () -> log.warn("Webhook for unknown checkout session"));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(PaymentResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsForUser(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(PaymentResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal getWalletBalance(Long userId) {
        return userRepository.findById(userId)
            .map(User::getWalletBalance)
            .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    private void applySession(
            Long paymentId,
            PaymentGateway.CheckoutSession session) {
        Payment payment = lockFresh(Payment.class, paymentId);

        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            return; // already settled: idempotent
        }
        if (!session.id().equals(payment.getStripeCheckoutSessionId())) {
            log.error("Session mismatch for payment {}", paymentId);
            return;
        }

        boolean paid = "complete".equals(session.status())
            && "paid".equals(session.paymentStatus());

        if (paid) {
            boolean amountMatches = session.amountTotal() != null
                && session.amountTotal() == toMinorUnits(payment.getAmount())
                && payment.getCurrency().equalsIgnoreCase(
                    String.valueOf(session.currency()));
            if (!amountMatches) {
                log.error("Amount/currency mismatch for payment {}: "
                    + "flagging as FAILED for manual review", paymentId);
                payment.setStatus(Payment.PaymentStatus.FAILED);
                paymentRepository.save(payment);
                return;
            }
            credit(payment, session.paymentIntentId());
        } else if ("expired".equals(session.status())) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
        }
    }

    private void credit(Payment payment, String paymentIntentId) {
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        payment.setStripePaymentIntentId(paymentIntentId);
        paymentRepository.save(payment);

        User user = lockFresh(User.class, payment.getUser().getId());
        user.setWalletBalance(user.getWalletBalance().add(payment.getAmount()));
        userRepository.save(user);

        Long userId = user.getId();
        String email = user.getEmail();
        String username = user.getUsername();
        BigDecimal amount = payment.getAmount();
        BigDecimal newBalance = user.getWalletBalance();
        AfterCommit.run(() -> {
            webSocketService.sendWalletUpdate(userId, newBalance);
            emailService.sendWalletTopUpEmail(
                email, username, amount, newBalance);
        });
    }

    /**
     * SELECT ... FOR UPDATE that also overwrites any copy already in the
     * persistence context. A plain locking query would return the stale
     * in-memory instance, so two concurrent syncs could both see PENDING
     * and credit the wallet twice.
     */
    private <T> T lockFresh(Class<T> type, Long id) {
        T entity = entityManager.find(type, id);
        if (entity == null) {
            throw new IllegalStateException(
                type.getSimpleName() + " " + id + " disappeared");
        }
        entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
        return entity;
    }

    private static long toMinorUnits(BigDecimal amount) {
        return amount.movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact();
    }

    private static String reference(Long paymentId, String sessionId) {
        String tail = sessionId.length() > 8
            ? sessionId.substring(sessionId.length() - 8)
            : sessionId;
        return ("LL-" + paymentId + "-" + tail).toUpperCase(Locale.ROOT);
    }
}
