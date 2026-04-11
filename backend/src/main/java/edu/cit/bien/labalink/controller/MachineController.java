package edu.cit.bien.labalink.controller;

import edu.cit.bien.labalink.dto.ApiResponse;
import edu.cit.bien.labalink.dto.MachineRequest;
import edu.cit.bien.labalink.dto.MachineResponse;
import edu.cit.bien.labalink.service.MachineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/machines")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class MachineController {

    private final MachineService machineService;

    @GetMapping
    public ResponseEntity<List<MachineResponse>> 
            getAllMachines() {
        return ResponseEntity.ok(
            machineService.getAllMachines());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMachineById(
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(
                machineService.getMachineById(id));
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(
                    false, e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createMachine(
            @Valid @RequestBody 
            MachineRequest request) {
        try {
            MachineResponse response = 
                machineService.createMachine(request);
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse(
                    false, e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMachine(
            @PathVariable Long id,
            @Valid @RequestBody 
            MachineRequest request) {
        try {
            return ResponseEntity.ok(
                machineService.updateMachine(
                    id, request));
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(
                    false, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMachine(
            @PathVariable Long id) {
        try {
            machineService.deleteMachine(id);
            return ResponseEntity.ok(
                new ApiResponse(true, 
                    "Machine deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse(
                    false, e.getMessage()));
        }
    }
}