package edu.cit.bien.labalink.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt;

    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public enum BookingStatus {
        PENDING, ACTIVE, COMPLETED, CANCELLED;

        public boolean isOpen() {
            return this == PENDING || this == ACTIVE;
        }

        public boolean canTransitionTo(BookingStatus next) {
            return switch (this) {
                case PENDING -> next == ACTIVE || next == CANCELLED;
                case ACTIVE -> next == COMPLETED || next == CANCELLED;
                case COMPLETED, CANCELLED -> false;
            };
        }
    }
}
