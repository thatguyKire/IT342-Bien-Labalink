package edu.cit.bien.labalink.dto;

import edu.cit.bien.labalink.model.Machine;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class MachineRequest {

    @NotBlank(message = "Machine name is required")
    @Size(max = 100, message = "Machine name is too long")
    @Pattern(regexp = "^[\\p{L}\\p{N} #()._-]+$",
             message = "Machine name contains invalid characters")
    private String machineName;

    @NotNull(message = "Machine type is required")
    private Machine.MachineType machineType;

    @NotNull(message = "Hourly rate is required")
    @DecimalMin(value = "1.00",
                message = "Hourly rate must be at least 1.00")
    @DecimalMax(value = "10000.00",
                message = "Hourly rate is too high")
    @Digits(integer = 5, fraction = 2,
            message = "Hourly rate may have at most 2 decimals")
    private BigDecimal hourlyRate;

    private Machine.MachineStatus status;
}
