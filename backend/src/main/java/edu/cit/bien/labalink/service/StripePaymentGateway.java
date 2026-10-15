package edu.cit.bien.labalink.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import edu.cit.bien.labalink.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class StripePaymentGateway implements PaymentGateway {

    private final AppProperties properties;

    @Override
    public boolean isConfigured() {
        return properties.stripe().isConfigured();
    }

    @Override
    public CheckoutSession createCheckoutSession(
            long paymentId,
            long userId,
            String customerEmail,
            long amountMinor,
            String currency,
            String idempotencyKey) {

        SessionCreateParams params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl(properties.stripe().successUrl()
                + "?session_id={CHECKOUT_SESSION_ID}")
            .setCancelUrl(properties.stripe().cancelUrl())
            .setClientReferenceId(String.valueOf(paymentId))
            .setCustomerEmail(customerEmail)
            .setExpiresAt(Instant.now()
                .plus(30, ChronoUnit.MINUTES).getEpochSecond())
            .putMetadata("payment_id", String.valueOf(paymentId))
            .putMetadata("user_id", String.valueOf(userId))
            .addLineItem(SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                    .setCurrency(currency)
                    .setUnitAmount(amountMinor)
                    .setProductData(SessionCreateParams.LineItem.PriceData
                        .ProductData.builder()
                        .setName("LabaLink Wallet Top-Up")
                        .build())
                    .build())
                .build())
            .build();

        try {
            Session session = Session.create(params, RequestOptions.builder()
                .setApiKey(properties.stripe().secretKey())
                .setIdempotencyKey(idempotencyKey)
                .build());
            return map(session);
        } catch (StripeException e) {
            throw new PaymentProviderException(
                "Stripe checkout creation failed", e);
        }
    }

    @Override
    public CheckoutSession retrieveCheckoutSession(String sessionId) {
        try {
            return map(Session.retrieve(sessionId, RequestOptions.builder()
                .setApiKey(properties.stripe().secretKey())
                .build()));
        } catch (StripeException e) {
            throw new PaymentProviderException(
                "Stripe session lookup failed", e);
        }
    }

    @Override
    public WebhookEvent parseWebhook(String payload, String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(
                payload, signatureHeader,
                properties.stripe().webhookSecret());
        } catch (SignatureVerificationException e) {
            throw new IllegalArgumentException("Invalid Stripe signature", e);
        }

        if (!event.getType().startsWith("checkout.session.")) {
            return null;
        }
        StripeObject object = event.getDataObjectDeserializer()
            .getObject()
            .orElse(null);
        if (object == null) {
            try {
                // API version mismatch between account and SDK.
                object = event.getDataObjectDeserializer().deserializeUnsafe();
            } catch (Exception e) {
                throw new PaymentProviderException(
                    "Could not parse Stripe event", e);
            }
        }
        if (!(object instanceof Session session)) {
            return null;
        }
        return new WebhookEvent(event.getType(), map(session));
    }

    private static CheckoutSession map(Session session) {
        return new CheckoutSession(
            session.getId(),
            session.getUrl(),
            session.getStatus(),
            session.getPaymentStatus(),
            session.getAmountTotal(),
            session.getCurrency(),
            session.getPaymentIntent(),
            session.getMetadata());
    }
}
