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
@Table(name = "machines")
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String machineName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MachineType machineType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MachineStatus status = MachineStatus.AVAILABLE;

    // Secret printed on the machine's QR sticker. Proves physical presence,
    // so it is only ever returned to attendants.
    @Column(nullable = false, unique = true, length = 64)
    private String qrToken;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt;

    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public enum MachineType {
        WASHER, DRYER
    }

    public enum MachineStatus {
        AVAILABLE, RUNNING, OUT_OF_ORDER
    }
}
