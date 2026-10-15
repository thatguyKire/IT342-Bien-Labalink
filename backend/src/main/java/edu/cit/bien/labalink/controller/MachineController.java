package edu.cit.bien.labalink.controller;

import edu.cit.bien.labalink.dto.ApiResponse;
import edu.cit.bien.labalink.dto.MachineRequest;
import edu.cit.bien.labalink.dto.MachineResponse;
import edu.cit.bien.labalink.security.UserPrincipal;
import edu.cit.bien.labalink.service.MachineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/machines")
@RequiredArgsConstructor
public class MachineController {

    private final MachineService machineService;

    /** Any signed-in user; QR tokens are included for attendants only. */
    @GetMapping
    public List<MachineResponse> getAllMachines(
            @AuthenticationPrincipal UserPrincipal principal) {
        return machineService.getAllMachines(principal.isAttendant());
    }

    @GetMapping("/{id}")
    public MachineResponse getMachine(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return machineService.getMachineById(id, principal.isAttendant());
    }

    @PostMapping
    @PreAuthorize("hasRole('ATTENDANT')")
    @ResponseStatus(HttpStatus.CREATED)
    public MachineResponse createMachine(
            @Valid @RequestBody MachineRequest request) {
        return machineService.createMachine(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ATTENDANT')")
    public MachineResponse updateMachine(
            @PathVariable Long id,
            @Valid @RequestBody MachineRequest request) {
        return machineService.updateMachine(id, request);
    }

    @PostMapping("/{id}/regenerate-qr")
    @PreAuthorize("hasRole('ATTENDANT')")
    public MachineResponse regenerateQr(@PathVariable Long id) {
        return machineService.regenerateQrToken(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ATTENDANT')")
    public ApiResponse deleteMachine(@PathVariable Long id) {
        machineService.deleteMachine(id);
        return new ApiResponse(true, "Machine deleted successfully");
    }
}
