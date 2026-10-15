package edu.cit.bien.labalink.support;

import edu.cit.bien.labalink.service.PaymentGateway;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** In-memory stand-in for Stripe Checkout. */
public class FakePaymentGateway implements PaymentGateway {

    private final Map<String, CheckoutSession> sessions =
        new ConcurrentHashMap<>();
    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public CheckoutSession createCheckoutSession(
            long paymentId, long userId, String customerEmail,
            long amountMinor, String currency, String idempotencyKey) {
        String id = "cs_test_" + counter.incrementAndGet();
        CheckoutSession session = new CheckoutSession(
            id, "https://checkout.stripe.test/" + id, "open", "unpaid",
            amountMinor, currency, null,
            Map.of("payment_id", String.valueOf(paymentId)));
        sessions.put(id, session);
        return session;
    }

    @Override
    public CheckoutSession retrieveCheckoutSession(String sessionId) {
        return sessions.get(sessionId);
    }

    @Override
    public WebhookEvent parseWebhook(String payload, String signature) {
        if (!"valid-signature".equals(signature)) {
            throw new IllegalArgumentException("bad signature");
        }
        CheckoutSession session = sessions.get(payload);
        return session == null ? null
            : new WebhookEvent("checkout.session.completed", session);
    }

    public void markPaid(String sessionId) {
        markPaid(sessionId, sessions.get(sessionId).amountTotal());
    }

    public void markPaid(String sessionId, long amountMinor) {
        CheckoutSession s = sessions.get(sessionId);
        sessions.put(sessionId, new CheckoutSession(
            s.id(), s.url(), "complete", "paid", amountMinor,
            s.currency(), "pi_" + sessionId, s.metadata()));
    }

    public String lastSessionId() {
        return "cs_test_" + counter.get();
    }
}
