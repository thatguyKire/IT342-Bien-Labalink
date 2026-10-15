package edu.cit.bien.labalink.controller;

import edu.cit.bien.labalink.dto.ApiResponse;
import edu.cit.bien.labalink.dto.PaymentResponse;
import edu.cit.bien.labalink.dto.TopUpRequest;
import edu.cit.bien.labalink.dto.WalletResponse;
import edu.cit.bien.labalink.security.UserPrincipal;
import edu.cit.bien.labalink.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /** Starts a Stripe Checkout top-up; returns the hosted payment URL. */
    @PostMapping("/checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse createCheckout(
            @Valid @RequestBody TopUpRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.createCheckout(principal, request.getAmount());
    }

    /** Re-checks a payment with Stripe and credits the wallet if paid. */
    @PostMapping("/{id}/sync")
    public PaymentResponse sync(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.syncPayment(id, principal);
    }

    @GetMapping("/me")
    public List<PaymentResponse> myPayments(
            @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.getPaymentsForUser(principal.id());
    }

    @GetMapping("/wallet")
    public WalletResponse myWallet(
            @AuthenticationPrincipal UserPrincipal principal) {
        return new WalletResponse(
            paymentService.getWalletBalance(principal.id()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ATTENDANT')")
    public List<PaymentResponse> allPayments() {
        return paymentService.getAllPayments();
    }

    /** Stripe → server. Authenticated by the Stripe-Signature HMAC. */
    @PostMapping("/webhook")
    public ApiResponse webhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false)
            String signature) {
        paymentService.handleWebhook(payload, signature);
        return new ApiResponse(true, "ok");
    }
}
