package edu.cit.bien.labalink.machine;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class MachineRequest {

    @NotBlank(message = "Machine name is required")
    private String machineName;

    @NotNull(message = "Machine type is required")
    private Machine.MachineType machineType;

    @NotNull(message = "Hourly rate is required")
    @Positive(message = "Hourly rate must be positive")
    private Double hourlyRate;

    private Machine.MachineStatus status 
        = Machine.MachineStatus.AVAILABLE;
}