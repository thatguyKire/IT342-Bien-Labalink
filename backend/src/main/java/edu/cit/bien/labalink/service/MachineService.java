package edu.cit.bien.labalink.service;

import edu.cit.bien.labalink.dto.MachineRequest;
import edu.cit.bien.labalink.dto.MachineResponse;
import edu.cit.bien.labalink.exception.ApiException;
import edu.cit.bien.labalink.model.Machine;
import edu.cit.bien.labalink.repository.BookingRepository;
import edu.cit.bien.labalink.repository.MachineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MachineService {

    private final MachineRepository machineRepository;
    private final BookingRepository bookingRepository;
    private final WebSocketService webSocketService;
    private final SecureRandom random = new SecureRandom();

    @Transactional(readOnly = true)
    public List<MachineResponse> getAllMachines(boolean includeQrToken) {
        return machineRepository.findAllByOrderByIdAsc()
            .stream()
            .map(m -> MachineResponse.fromEntity(m, includeQrToken))
            .toList();
    }

    @Transactional(readOnly = true)
    public MachineResponse getMachineById(Long id, boolean includeQrToken) {
        return MachineResponse.fromEntity(find(id), includeQrToken);
    }

    @Transactional
    public MachineResponse createMachine(MachineRequest request) {
        Machine machine = new Machine();
        apply(machine, request);
        if (machine.getStatus() == null) {
            machine.setStatus(Machine.MachineStatus.AVAILABLE);
        }
        machine.setQrToken(newUniqueQrToken());
        return saveAndBroadcast(machine);
    }

    @Transactional
    public MachineResponse updateMachine(Long id, MachineRequest request) {
        Machine machine = machineRepository.findByIdForUpdate(id)
            .orElseThrow(() -> ApiException.notFound("Machine not found"));
        apply(machine, request);
        return saveAndBroadcast(machine);
    }

    /** Invalidates the printed sticker, e.g. if a photo of it leaked. */
    @Transactional
    public MachineResponse regenerateQrToken(Long id) {
        Machine machine = machineRepository.findByIdForUpdate(id)
            .orElseThrow(() -> ApiException.notFound("Machine not found"));
        machine.setQrToken(newUniqueQrToken());
        return saveAndBroadcast(machine);
    }

    @Transactional
    public void deleteMachine(Long id) {
        Machine machine = find(id);
        if (bookingRepository.existsByMachineId(id)) {
            throw ApiException.conflict(
                "This machine has booking history and cannot be deleted. "
                + "Mark it Out of Order instead.");
        }
        machineRepository.delete(machine);
        AfterCommit.run(() -> webSocketService.broadcastMachineUpdate(
            Map.of("id", id, "deleted", true)));
    }

    private Machine find(Long id) {
        return machineRepository.findById(id)
            .orElseThrow(() -> ApiException.notFound("Machine not found"));
    }

    private void apply(Machine machine, MachineRequest request) {
        machine.setMachineName(request.getMachineName().trim());
        machine.setMachineType(request.getMachineType());
        machine.setHourlyRate(
            request.getHourlyRate().setScale(2, RoundingMode.HALF_UP));
        if (request.getStatus() != null) {
            machine.setStatus(request.getStatus());
        }
    }

    private MachineResponse saveAndBroadcast(Machine machine) {
        Machine saved = machineRepository.save(machine);
        // The broadcast goes to every signed-in user, so it must never
        // include the QR secret.
        MachineResponse publicView = MachineResponse.fromEntity(saved, false);
        AfterCommit.run(() ->
            webSocketService.broadcastMachineUpdate(publicView));
        return MachineResponse.fromEntity(saved, true);
    }

    // 144 bits of randomness: cannot be guessed, only scanned.
    private String newUniqueQrToken() {
        String token;
        do {
            byte[] bytes = new byte[18];
            random.nextBytes(bytes);
            token = "LL-" + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
        } while (machineRepository.existsByQrToken(token));
        return token;
    }
}
