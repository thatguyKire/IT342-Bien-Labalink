package edu.cit.bien.labalink.booking;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

import edu.cit.bien.labalink.auth.User;
import edu.cit.bien.labalink.machine.Machine;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
    @Column(nullable = false)
    private BookingStatus status = 
        BookingStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    @Column(nullable = false)
    private Double totalPrice;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = 
        LocalDateTime.now();

    public enum BookingStatus {
        PENDING, ACTIVE, COMPLETED, CANCELLED
    }
}