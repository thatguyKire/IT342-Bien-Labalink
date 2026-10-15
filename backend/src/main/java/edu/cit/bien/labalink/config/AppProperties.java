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
