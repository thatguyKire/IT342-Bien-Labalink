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
@Table(name = "machines")
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String machineName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MachineType machineType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MachineStatus status = MachineStatus.AVAILABLE;

    @Column(nullable = false, unique = true)
    private String qrToken;

    @Column(nullable = false)
    private Double hourlyRate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum MachineType {
        WASHER, DRYER
    }

    public enum MachineStatus {
        AVAILABLE, RUNNING, OUT_OF_ORDER
    }
}