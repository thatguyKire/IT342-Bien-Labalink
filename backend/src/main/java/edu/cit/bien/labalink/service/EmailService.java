package edu.cit.bien.labalink.service;

import edu.cit.bien.labalink.model.EmailLog;
import edu.cit.bien.labalink.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailLogRepository 
        emailLogRepository;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.name}")
    private String fromName;

    @Async
    public void sendWelcomeEmail(
            String toEmail,
            String username) {

        String subject = 
            "Welcome to LabaLink! 🫧";

        String body = buildWelcomeEmail(username);

        sendEmail(
            toEmail,
            subject,
            body,
            EmailLog.EmailType.WELCOME);
    }

    @Async
    public void sendBookingConfirmationEmail(
            String toEmail,
            String username,
            String machineName,
            String machineType,
            String startTime,
            Double totalPrice) {

        String subject = 
            "Booking Confirmed - LabaLink 🫧";

        String body = buildBookingConfirmationEmail(
            username,
            machineName,
            machineType,
            startTime,
            totalPrice);

        sendEmail(
            toEmail,
            subject,
            body,
            EmailLog.EmailType.BOOKING_CONFIRMATION);
    }

    @Async
    public void sendBookingCompletedEmail(
            String toEmail,
            String username,
            String machineName,
            Double totalPrice) {

        String subject = 
            "Session Completed - LabaLink 🫧";

        String body = buildBookingCompletedEmail(
            username,
            machineName,
            totalPrice);

        sendEmail(
            toEmail,
            subject,
            body,
            EmailLog.EmailType.BOOKING_COMPLETED);
    }

    @Async
    public void sendWalletTopUpEmail(
            String toEmail,
            String username,
            Double amount,
            Double newBalance) {

        String subject = 
            "Wallet Top-Up Successful - LabaLink 🫧";

        String body = buildWalletTopUpEmail(
            username,
            amount,
            newBalance);

        sendEmail(
            toEmail,
            subject,
            body,
            EmailLog.EmailType.WALLET_TOPUP);
    }

    private void sendEmail(
            String toEmail,
            String subject,
            String htmlBody,
            EmailLog.EmailType emailType) {

        EmailLog emailLog = new EmailLog();
        emailLog.setToEmail(toEmail);
        emailLog.setSubject(subject);
        emailLog.setEmailType(emailType);

        try {
            MimeMessage message = 
                mailSender.createMimeMessage();
            MimeMessageHelper helper =
                new MimeMessageHelper(
                    message, true, "UTF-8");

            helper.setFrom(
                fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);

            emailLog.setStatus(
                EmailLog.EmailStatus.SENT);
            log.info(
                "Email sent to: {}", toEmail);

        } catch (Exception e) {
            emailLog.setStatus(
                EmailLog.EmailStatus.FAILED);
            emailLog.setErrorMessage(
                e.getMessage());
            log.error(
                "Failed to send email to {}: {}",
                toEmail, e.getMessage());
        }

        emailLogRepository.save(emailLog);
    }

    // Email Templates
    private String buildWelcomeEmail(
            String username) {
        return """
            <div style="font-family: Arial, sans-serif;
                        max-width: 600px;
                        margin: 0 auto;
                        background: #F5F3FF;
                        padding: 40px 20px;">
              <div style="background: white;
                          border-radius: 24px;
                          padding: 40px;
                          text-align: center;">
                <div style="font-size: 48px;
                            margin-bottom: 16px;">
                  🫧
                </div>
                <h1 style="color: #9333EA;
                           font-size: 28px;
                           margin-bottom: 8px;">
                  Welcome to LabaLink!
                </h1>
                <p style="color: #6B7280;
                          font-size: 16px;
                          margin-bottom: 24px;">
                  Hi <strong>%s</strong>! 
                  Your account has been created 
                  successfully.
                </p>
                <div style="background: #F5F3FF;
                            border-radius: 16px;
                            padding: 20px;
                            margin-bottom: 24px;">
                  <p style="color: #374151;
                            font-size: 14px;
                            margin: 0;">
                    You can now book laundry machines,
                    track your sessions, and manage 
                    your wallet all in one place.
                  </p>
                </div>
                <p style="color: #9CA3AF;
                          font-size: 12px;">
                  © 2026 LabaLink. 
                  All rights reserved.
                </p>
              </div>
            </div>
            """.formatted(username);
    }

    private String buildBookingConfirmationEmail(
            String username,
            String machineName,
            String machineType,
            String startTime,
            Double totalPrice) {
        return """
            <div style="font-family: Arial, sans-serif;
                        max-width: 600px;
                        margin: 0 auto;
                        background: #F5F3FF;
                        padding: 40px 20px;">
              <div style="background: white;
                          border-radius: 24px;
                          padding: 40px;">
                <div style="text-align: center;
                            margin-bottom: 24px;">
                  <div style="font-size: 48px;">
                    🫧
                  </div>
                  <h1 style="color: #9333EA;
                             font-size: 24px;">
                    Booking Confirmed!
                  </h1>
                </div>
                <p style="color: #374151;">
                  Hi <strong>%s</strong>,
                  your booking has been confirmed.
                </p>
                <div style="background: #F5F3FF;
                            border-radius: 16px;
                            padding: 20px;
                            margin: 20px 0;">
                  <table style="width: 100%%;
                                border-collapse: collapse;">
                    <tr>
                      <td style="color: #6B7280;
                                 padding: 8px 0;
                                 font-size: 14px;">
                        Machine
                      </td>
                      <td style="color: #374151;
                                 font-weight: bold;
                                 text-align: right;
                                 font-size: 14px;">
                        %s (%s)
                      </td>
                    </tr>
                    <tr>
                      <td style="color: #6B7280;
                                 padding: 8px 0;
                                 font-size: 14px;">
                        Start Time
                      </td>
                      <td style="color: #374151;
                                 font-weight: bold;
                                 text-align: right;
                                 font-size: 14px;">
                        %s
                      </td>
                    </tr>
                    <tr>
                      <td style="color: #6B7280;
                                 padding: 8px 0;
                                 font-size: 14px;">
                        Total Price
                      </td>
                      <td style="color: #9333EA;
                                 font-weight: bold;
                                 text-align: right;
                                 font-size: 16px;">
                        ₱%.2f
                      </td>
                    </tr>
                  </table>
                </div>
                <p style="color: #9CA3AF;
                          font-size: 12px;
                          text-align: center;">
                  © 2026 LabaLink. 
                  All rights reserved.
                </p>
              </div>
            </div>
            """.formatted(
                username,
                machineName,
                machineType,
                startTime,
                totalPrice);
    }

    private String buildBookingCompletedEmail(
            String username,
            String machineName,
            Double totalPrice) {
        return """
            <div style="font-family: Arial, sans-serif;
                        max-width: 600px;
                        margin: 0 auto;
                        background: #F5F3FF;
                        padding: 40px 20px;">
              <div style="background: white;
                          border-radius: 24px;
                          padding: 40px;
                          text-align: center;">
                <div style="font-size: 48px;
                            margin-bottom: 16px;">
                  ✅
                </div>
                <h1 style="color: #10B981;
                           font-size: 24px;">
                  Session Completed!
                </h1>
                <p style="color: #374151;">
                  Hi <strong>%s</strong>, 
                  your laundry session on 
                  <strong>%s</strong> 
                  has been completed.
                </p>
                <div style="background: #F0FDF4;
                            border-radius: 16px;
                            padding: 16px;
                            margin: 20px 0;">
                  <p style="color: #10B981;
                            font-size: 20px;
                            font-weight: bold;
                            margin: 0;">
                    Total: ₱%.2f
                  </p>
                </div>
                <p style="color: #6B7280;
                          font-size: 14px;">
                  Thank you for using LabaLink! 
                  Your laundry is done. 🫧
                </p>
                <p style="color: #9CA3AF;
                          font-size: 12px;">
                  © 2026 LabaLink.
                  All rights reserved.
                </p>
              </div>
            </div>
            """.formatted(
                username,
                machineName,
                totalPrice);
    }

    private String buildWalletTopUpEmail(
            String username,
            Double amount,
            Double newBalance) {
        return """
            <div style="font-family: Arial, sans-serif;
                        max-width: 600px;
                        margin: 0 auto;
                        background: #F5F3FF;
                        padding: 40px 20px;">
              <div style="background: white;
                          border-radius: 24px;
                          padding: 40px;
                          text-align: center;">
                <div style="font-size: 48px;
                            margin-bottom: 16px;">
                  💜
                </div>
                <h1 style="color: #9333EA;
                           font-size: 24px;">
                  Wallet Top-Up Successful!
                </h1>
                <p style="color: #374151;">
                  Hi <strong>%s</strong>,
                  your wallet has been topped up.
                </p>
                <div style="background: #F5F3FF;
                            border-radius: 16px;
                            padding: 20px;
                            margin: 20px 0;">
                  <p style="color: #6B7280;
                            font-size: 14px;
                            margin: 0 0 8px 0;">
                    Amount Added
                  </p>
                  <p style="color: #9333EA;
                            font-size: 28px;
                            font-weight: bold;
                            margin: 0 0 16px 0;">
                    + ₱%.2f
                  </p>
                  <p style="color: #6B7280;
                            font-size: 14px;
                            margin: 0 0 8px 0;">
                    New Balance
                  </p>
                  <p style="color: #374151;
                            font-size: 24px;
                            font-weight: bold;
                            margin: 0;">
                    ₱%.2f
                  </p>
                </div>
                <p style="color: #9CA3AF;
                          font-size: 12px;">
                  © 2026 LabaLink.
                  All rights reserved.
                </p>
              </div>
            </div>
            """.formatted(
                username,
                amount,
                newBalance);
    }
}