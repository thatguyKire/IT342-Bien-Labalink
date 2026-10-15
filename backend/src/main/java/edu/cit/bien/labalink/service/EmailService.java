package edu.cit.bien.labalink.service;

import edu.cit.bien.labalink.config.AppProperties;
import edu.cit.bien.labalink.model.EmailLog;
import edu.cit.bien.labalink.repository.EmailLogRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Transactional emails. Every user-controlled value is HTML-escaped before
 * it is placed in a template (usernames used to be inserted raw, which
 * allowed HTML/phishing-link injection into LabaLink-branded mail).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailLogRepository emailLogRepository;
    private final AppProperties properties;

    @Async
    public void sendWelcomeEmail(String toEmail, String username) {
        String body = card("🫧", "#9333EA", "Welcome to LabaLink!", """
            <p style="color:#6B7280;font-size:16px;">
              Hi <strong>%s</strong>! Your account has been created.
            </p>
            <div style="background:#F5F3FF;border-radius:16px;padding:20px;">
              <p style="color:#374151;font-size:14px;margin:0;">
                You can now book laundry machines, track your sessions,
                and manage your wallet all in one place.
              </p>
            </div>
            """.formatted(esc(username)));
        send(toEmail, "Welcome to LabaLink! 🫧", body,
            EmailLog.EmailType.WELCOME);
    }

    @Async
    public void sendBookingConfirmationEmail(
            String toEmail,
            String username,
            String machineName,
            String machineType,
            String startTime,
            BigDecimal totalPrice) {
        String body = card("🫧", "#9333EA", "Booking Confirmed!", """
            <p style="color:#374151;">
              Hi <strong>%s</strong>, your booking has been confirmed.
            </p>
            <table style="width:100%%;background:#F5F3FF;border-radius:16px;
                          padding:20px;border-collapse:separate;">
              %s%s%s
            </table>
            """.formatted(
                esc(username),
                row("Machine", esc(machineName) + " (" + esc(machineType) + ")"),
                row("Start Time", esc(startTime)),
                row("Total Price", "₱" + money(totalPrice))));
        send(toEmail, "Booking Confirmed - LabaLink 🫧", body,
            EmailLog.EmailType.BOOKING_CONFIRMATION);
    }

    @Async
    public void sendBookingCompletedEmail(
            String toEmail,
            String username,
            String machineName,
            BigDecimal totalPrice) {
        String body = card("✅", "#10B981", "Session Completed!", """
            <p style="color:#374151;">
              Hi <strong>%s</strong>, your laundry session on
              <strong>%s</strong> has been completed.
            </p>
            <div style="background:#F0FDF4;border-radius:16px;padding:16px;">
              <p style="color:#10B981;font-size:20px;font-weight:bold;margin:0;">
                Total: ₱%s
              </p>
            </div>
            <p style="color:#6B7280;font-size:14px;">
              Thank you for using LabaLink! Your laundry is done. 🫧
            </p>
            """.formatted(esc(username), esc(machineName), money(totalPrice)));
        send(toEmail, "Session Completed - LabaLink 🫧", body,
            EmailLog.EmailType.BOOKING_COMPLETED);
    }

    @Async
    public void sendWalletTopUpEmail(
            String toEmail,
            String username,
            BigDecimal amount,
            BigDecimal newBalance) {
        String body = card("💜", "#9333EA", "Wallet Top-Up Successful!", """
            <p style="color:#374151;">
              Hi <strong>%s</strong>, your wallet has been topped up.
            </p>
            <div style="background:#F5F3FF;border-radius:16px;padding:20px;">
              <p style="color:#6B7280;font-size:14px;margin:0 0 8px 0;">Amount Added</p>
              <p style="color:#9333EA;font-size:28px;font-weight:bold;margin:0 0 16px 0;">
                + ₱%s
              </p>
              <p style="color:#6B7280;font-size:14px;margin:0 0 8px 0;">New Balance</p>
              <p style="color:#374151;font-size:24px;font-weight:bold;margin:0;">
                ₱%s
              </p>
            </div>
            """.formatted(esc(username), money(amount), money(newBalance)));
        send(toEmail, "Wallet Top-Up Successful - LabaLink 🫧", body,
            EmailLog.EmailType.WALLET_TOPUP);
    }

    private void send(
            String toEmail,
            String subject,
            String htmlBody,
            EmailLog.EmailType emailType) {

        EmailLog emailLog = new EmailLog();
        emailLog.setToEmail(toEmail);
        emailLog.setSubject(subject);
        emailLog.setEmailType(emailType);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(properties.email().from(), properties.email().name());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            emailLog.setStatus(EmailLog.EmailStatus.SENT);
        } catch (Exception e) {
            emailLog.setStatus(EmailLog.EmailStatus.FAILED);
            String error = String.valueOf(e.getMessage());
            emailLog.setErrorMessage(
                error.length() > 1000 ? error.substring(0, 1000) : error);
            log.warn("Failed to send {} email: {}", emailType, error);
        }

        try {
            emailLogRepository.save(emailLog);
        } catch (Exception e) {
            log.warn("Failed to record email log: {}", e.getMessage());
        }
    }

    private static String card(
            String icon, String color, String title, String content) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;
                        background:#F5F3FF;padding:40px 20px;">
              <div style="background:white;border-radius:24px;padding:40px;">
                <div style="text-align:center;margin-bottom:24px;">
                  <div style="font-size:48px;">%s</div>
                  <h1 style="color:%s;font-size:24px;">%s</h1>
                </div>
                %s
                <p style="color:#9CA3AF;font-size:12px;text-align:center;margin-top:24px;">
                  © 2026 LabaLink. All rights reserved.
                </p>
              </div>
            </div>
            """.formatted(icon, color, title, content);
    }

    private static String row(String label, String value) {
        return """
            <tr>
              <td style="color:#6B7280;padding:8px 0;font-size:14px;">%s</td>
              <td style="color:#374151;font-weight:bold;text-align:right;font-size:14px;">%s</td>
            </tr>
            """.formatted(label, value);
    }

    private static String esc(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }

    private static String money(BigDecimal value) {
        return value == null ? "0.00"
            : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
