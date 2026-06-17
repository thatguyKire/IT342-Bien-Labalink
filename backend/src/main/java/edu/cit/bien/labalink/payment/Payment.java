package edu.cit.bien.labalink.payment;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

import edu.cit.bien.labalink.auth.User;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Double amount;

    @Column
    private String stripePaymentIntentId;

    @Column
    private String providerReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = 
        PaymentStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = 
        LocalDateTime.now();

    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED
    }
}