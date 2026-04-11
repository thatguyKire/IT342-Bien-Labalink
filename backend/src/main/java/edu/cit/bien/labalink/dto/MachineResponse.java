package edu.cit.bien.labalink.dto;

import edu.cit.bien.labalink.model.Machine;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MachineResponse {
    private Long id;
    private String machineName;
    private Machine.MachineType machineType;
    private Machine.MachineStatus status;
    private String qrToken;
    private Double hourlyRate;
    private LocalDateTime createdAt;

    public static MachineResponse fromEntity(
            Machine machine) {
        MachineResponse response = new MachineResponse();
        response.setId(machine.getId());
        response.setMachineName(machine.getMachineName());
        response.setMachineType(machine.getMachineType());
        response.setStatus(machine.getStatus());
        response.setQrToken(machine.getQrToken());
        response.setHourlyRate(machine.getHourlyRate());
        response.setCreatedAt(machine.getCreatedAt());
        return response;
    }
}