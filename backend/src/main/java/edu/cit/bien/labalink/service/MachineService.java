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

    private final MachineRepository machineRepository;

    public List<MachineResponse> getAllMachines() {
        return machineRepository.findAll()
                .stream()
                .map(MachineResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public MachineResponse getMachineById(Long id) {
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
        machine.setMachineName(request.getMachineName());
        machine.setMachineType(request.getMachineType());
        machine.setHourlyRate(request.getHourlyRate());
        machine.setStatus(request.getStatus() != null 
            ? request.getStatus() 
            : Machine.MachineStatus.AVAILABLE);

        // Generate unique QR token
        String qrToken;
        do {
            qrToken = "QR-" + UUID.randomUUID()
                .toString()
                .substring(0, 6)
                .toUpperCase();
        } while (machineRepository
            .existsByQrToken(qrToken));

        machine.setQrToken(qrToken);

        return MachineResponse.fromEntity(
            machineRepository.save(machine));
    }

    public MachineResponse updateMachine(
            Long id, MachineRequest request) {

        Machine machine = machineRepository
            .findById(id)
            .orElseThrow(() -> 
                new RuntimeException(
                    "Machine not found"));

        machine.setMachineName(request.getMachineName());
        machine.setMachineType(request.getMachineType());
        machine.setHourlyRate(request.getHourlyRate());
        if (request.getStatus() != null) {
            machine.setStatus(request.getStatus());
        }

        return MachineResponse.fromEntity(
            machineRepository.save(machine));
    }

    public void deleteMachine(Long id) {
        Machine machine = machineRepository
            .findById(id)
            .orElseThrow(() -> 
                new RuntimeException(
                    "Machine not found"));
        machineRepository.delete(machine);
    }
}