package edu.cit.bien.labalink.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.math.BigDecimal;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String frontendUrl,
        Cors cors,
        Jwt jwt,
        Google google,
        Stripe stripe,
        Wallet wallet,
        BookingRules booking,
        RateLimit rateLimit,
        Login login,
        Email email,
        Bootstrap bootstrap) {

    public record Cors(List<String> allowedOrigins) {}

    public record Jwt(String secret, String issuer, long expirationMinutes) {}

    public record Google(List<String> clientIds) {}

    public record Stripe(
            String secretKey,
            String webhookSecret,
            String currency,
            String successUrl,
            String cancelUrl) {

        /**
         * LabaLink is a class project: refuse live keys so it can never
         * charge real money. See docs/legal/Launch-Checklist.md before
         * removing this.
         */
        public Stripe {
            if (secretKey != null && (secretKey.startsWith("sk_live_")
                    || secretKey.startsWith("rk_live_"))) {
                throw new IllegalArgumentException(
                    "STRIPE_SECRET_KEY is a live key. LabaLink only accepts "
                    + "Stripe test keys (sk_test_...).");
            }
        }

        public boolean isConfigured() {
            return secretKey != null && !secretKey.isBlank();
        }
    }

    public record Wallet(
            BigDecimal minTopUp,
            BigDecimal maxTopUp,
            BigDecimal maxBalance) {}

    public record BookingRules(
            int minDurationMinutes,
            int maxDurationMinutes,
            int defaultDurationMinutes) {}

    public record RateLimit(
            int authRequestsPerMinute,
            int apiRequestsPerMinute) {}

    public record Login(int maxFailedAttempts, int lockoutMinutes) {}

    public record Email(String from, String name) {}

    public record Bootstrap(
            String attendantEmail,
            String attendantPassword,
            String attendantUsername) {}
}
