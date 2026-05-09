package edu.cit.bien.labalink.payment;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import edu.cit.bien.labalink.auth.User;
import edu.cit.bien.labalink.auth.UserRepository;
import edu.cit.bien.labalink.config.WebSocketService;
import edu.cit.bien.labalink.email.EmailService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository 
        paymentRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final WebSocketService webSocketService;

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public PaymentResponse createPaymentIntent(
            PaymentRequest request) {

        User user = userRepository
            .findById(request.getUserId())
            .orElseThrow(() ->
                new RuntimeException(
                    "User not found"));

        try {
            long amountInCentavos = 
                (long)(request.getAmount() * 100);

            PaymentIntentCreateParams params =
                PaymentIntentCreateParams.builder()
                    .setAmount(amountInCentavos)
                    .setCurrency("php")
                    .setDescription(
                        "LabaLink Wallet Top-Up for "
                        + user.getEmail())
                    .build();

            PaymentIntent paymentIntent =
                PaymentIntent.create(params);

            Payment payment = new Payment();
            payment.setUser(user);
            payment.setAmount(request.getAmount());
            payment.setStripePaymentIntentId(
                paymentIntent.getId());
            payment.setProviderReference(
                paymentIntent.getId()
                    .substring(0, 12)
                    .toUpperCase());
            payment.setStatus(
                Payment.PaymentStatus.PENDING);

            paymentRepository.save(payment);

            PaymentResponse response = 
                PaymentResponse.fromEntity(payment);
            response.setClientSecret(
                paymentIntent.getClientSecret());

            return response;

        } catch (StripeException e) {
            throw new RuntimeException(
                "Payment creation failed: " 
                + e.getMessage());
        }
    }

    public PaymentResponse confirmPayment(
            String paymentIntentId) {
        try {
            PaymentIntent paymentIntent =
                PaymentIntent.retrieve(
                    paymentIntentId);

            Payment payment = paymentRepository
                .findAll()
                .stream()
                .filter(p -> paymentIntentId.equals(
                    p.getStripePaymentIntentId()))
                .findFirst()
                .orElseThrow(() ->
                    new RuntimeException(
                        "Payment not found"));

            if ("succeeded".equals(
                    paymentIntent.getStatus())) {

                payment.setStatus(
                    Payment.PaymentStatus.SUCCESS);
                paymentRepository.save(payment);

                // Top up wallet balance
                User user = payment.getUser();
                user.setWalletBalance(
                    user.getWalletBalance() +
                    payment.getAmount());
                userRepository.save(user);

                // ✅ Send wallet top-up email
                emailService.sendWalletTopUpEmail(
                    user.getEmail(),
                    user.getUsername(),
                    payment.getAmount(),
                    user.getWalletBalance());

                // ✅ Broadcast wallet update
                webSocketService.broadcastWalletUpdate(
                    user.getEmail(),
                    user.getWalletBalance());

            } else {
                payment.setStatus(
                    Payment.PaymentStatus.FAILED);
                paymentRepository.save(payment);
            }

            return PaymentResponse
                .fromEntity(payment);

        } catch (StripeException e) {
            throw new RuntimeException(
                "Payment confirmation failed: "
                + e.getMessage());
        }
    }

    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll()
            .stream()
            .map(PaymentResponse::fromEntity)
            .collect(Collectors.toList());
    }

    public List<PaymentResponse> getPaymentsByUser(
            String email) {
        return paymentRepository
            .findByUserEmail(email)
            .stream()
            .map(PaymentResponse::fromEntity)
            .collect(Collectors.toList());
    }

    public Double getWalletBalance(Long userId) {
        User user = userRepository
            .findById(userId)
            .orElseThrow(() ->
                new RuntimeException(
                    "User not found"));
        return user.getWalletBalance();
    }
}