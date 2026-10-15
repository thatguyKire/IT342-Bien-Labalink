package edu.cit.bien.labalink.service;

import java.util.Map;

/**
 * Port to the payment provider (Stripe in production, a fake in tests).
 * The server only ever trusts what the provider reports, never the client.
 */
public interface PaymentGateway {

    record CheckoutSession(
            String id,
            String url,
            String status,          // open | complete | expired
            String paymentStatus,   // paid | unpaid | no_payment_required
            Long amountTotal,       // minor units (centavos)
            String currency,
            String paymentIntentId,
            Map<String, String> metadata) {}

    record WebhookEvent(String type, CheckoutSession session) {}

    boolean isConfigured();

    CheckoutSession createCheckoutSession(
            long paymentId,
            long userId,
            String customerEmail,
            long amountMinor,
            String currency,
            String idempotencyKey);

    CheckoutSession retrieveCheckoutSession(String sessionId);

    /**
     * Verifies the Stripe-Signature header and parses the event.
     * Throws IllegalArgumentException when the signature is invalid.
     * Returns null for event types that don't carry a Checkout Session.
     */
    WebhookEvent parseWebhook(String payload, String signatureHeader);

    class PaymentProviderException extends RuntimeException {
        public PaymentProviderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
