package edu.cit.bien.labalink.service;

import edu.cit.bien.labalink.dto.MachineRequest;
import edu.cit.bien.labalink.dto.MachineResponse;
import edu.cit.bien.labalink.model.Machine;
import edu.cit.bien.labalink.repository.MachineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MachineService {

    private final MachineRepository 
        machineRepository;
    private final WebSocketService 
        webSocketService;

    public List<MachineResponse> getAllMachines() {
        return machineRepository.findAll()
            .stream()
            .map(MachineResponse::fromEntity)
            .collect(Collectors.toList());
    }

    public MachineResponse getMachineById(
            Long id) {
        Machine machine = machineRepository
            .findById(id)
            .orElseThrow(() ->
                new RuntimeException(
                    "Machine not found"));
        return MachineResponse.fromEntity(machine);
    }

    public MachineResponse createMachine(
            MachineRequest request) {

        Machine machine = new Machine();
        machine.setMachineName(
            request.getMachineName());
        machine.setMachineType(
            request.getMachineType());
        machine.setHourlyRate(
            request.getHourlyRate());
        machine.setStatus(
            request.getStatus() != null
                ? request.getStatus()
                : Machine.MachineStatus.AVAILABLE);

        String qrToken;
        do {
            qrToken = "QR-" + UUID.randomUUID()
                .toString()
                .substring(0, 6)
                .toUpperCase();
        } while (machineRepository
            .existsByQrToken(qrToken));

        machine.setQrToken(qrToken);

        MachineResponse response =
            MachineResponse.fromEntity(
                machineRepository.save(machine));

        // ✅ Broadcast machine created
        webSocketService
            .broadcastMachineUpdate(response);

        return response;
    }

    public MachineResponse updateMachine(
            Long id,
            MachineRequest request) {

        Machine machine = machineRepository
            .findById(id)
            .orElseThrow(() ->
                new RuntimeException(
                    "Machine not found"));

        machine.setMachineName(
            request.getMachineName());
        machine.setMachineType(
            request.getMachineType());
        machine.setHourlyRate(
            request.getHourlyRate());
        if (request.getStatus() != null) {
            machine.setStatus(request.getStatus());
        }

        MachineResponse response =
            MachineResponse.fromEntity(
                machineRepository.save(machine));

        // ✅ Broadcast machine updated
        webSocketService
            .broadcastMachineUpdate(response);

        return response;
    }

    public void deleteMachine(Long id) {
        Machine machine = machineRepository
            .findById(id)
            .orElseThrow(() ->
                new RuntimeException(
                    "Machine not found"));
        machineRepository.delete(machine);

        // ✅ Broadcast machine deleted
        webSocketService.broadcastMachineUpdate(
            java.util.Map.of(
                "id", id,
                "deleted", true));
    }
}