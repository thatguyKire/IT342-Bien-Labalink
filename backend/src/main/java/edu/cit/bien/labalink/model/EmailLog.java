package edu.cit.bien.labalink.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "email_logs")
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 254)
    private String toEmail;

    @Column(nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EmailType emailType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailStatus status = EmailStatus.SENT;

    @Column(length = 1000)
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    public enum EmailType {
        WELCOME,
        BOOKING_CONFIRMATION,
        BOOKING_COMPLETED,
        WALLET_TOPUP
    }

    public enum EmailStatus {
        SENT, FAILED
    }
}
