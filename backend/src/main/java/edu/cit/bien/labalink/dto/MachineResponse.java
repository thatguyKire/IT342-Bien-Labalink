package edu.cit.bien.labalink.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import edu.cit.bien.labalink.model.Machine;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MachineResponse {
    private Long id;
    private String machineName;
    private Machine.MachineType machineType;
    private Machine.MachineStatus status;
    // Only populated for attendants; customers must scan the sticker.
    private String qrToken;
    private BigDecimal hourlyRate;
    private LocalDateTime createdAt;

    public static MachineResponse fromEntity(
            Machine machine, boolean includeQrToken) {
        MachineResponse response = new MachineResponse();
        response.setId(machine.getId());
        response.setMachineName(machine.getMachineName());
        response.setMachineType(machine.getMachineType());
        response.setStatus(machine.getStatus());
        if (includeQrToken) {
            response.setQrToken(machine.getQrToken());
        }
        response.setHourlyRate(machine.getHourlyRate());
        response.setCreatedAt(machine.getCreatedAt());
        return response;
    }
}
