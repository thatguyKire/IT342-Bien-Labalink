package edu.cit.bien.labalink.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "email_logs")
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String toEmail;

    @Column(nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailType emailType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailStatus status = 
        EmailStatus.SENT;

    @Column
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt = 
        LocalDateTime.now();

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