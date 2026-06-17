package edu.cit.bien.labalink.payment;

import edu.cit.bien.labalink.auth.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    // Create payment intent
    // Frontend calls this first to get clientSecret
    @PostMapping("/create-intent")
    public ResponseEntity<?> createPaymentIntent(
            @Valid @RequestBody
            PaymentRequest request) {
        try {
            PaymentResponse response =
                paymentService
                    .createPaymentIntent(request);
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(
                    false, e.getMessage()));
        }
    }

    // Confirm payment after Stripe processes it
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(
            @RequestBody 
            Map<String, String> body) {
        try {
            String paymentIntentId = 
                body.get("paymentIntentId");
            PaymentResponse response =
                paymentService
                    .confirmPayment(
                        paymentIntentId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(
                    false, e.getMessage()));
        }
    }

    // Get all payments (for attendant dashboard)
    @GetMapping
    public ResponseEntity<List<PaymentResponse>>
            getAllPayments() {
        return ResponseEntity.ok(
            paymentService.getAllPayments());
    }

    // Get payments by user email
    @GetMapping("/user/{email}")
    public ResponseEntity<List<PaymentResponse>>
            getPaymentsByUser(
                @PathVariable String email) {
        return ResponseEntity.ok(
            paymentService.getPaymentsByUser(email));
    }

    // Get wallet balance
    @GetMapping("/wallet/{userId}")
    public ResponseEntity<?> getWalletBalance(
            @PathVariable Long userId) {
        try {
            Double balance =
                paymentService
                    .getWalletBalance(userId);
            return ResponseEntity.ok(
                Map.of("walletBalance", balance));
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(
                    false, e.getMessage()));
        }
    }
}