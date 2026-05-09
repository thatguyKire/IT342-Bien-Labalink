package edu.cit.bien.labalink.machine;

import edu.cit.bien.labalink.dto.MachineRequest;
import edu.cit.bien.labalink.dto.MachineResponse;
import edu.cit.bien.labalink.model.Machine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MachineServiceTest {

    @Autowired
    private MachineService machineService;

    @Test
    void createMachine_GeneratesQrToken() {
        MachineRequest request = new MachineRequest();
        request.setMachineName("Test Washer");
        request.setMachineType(Machine.MachineType.WASHER);
        request.setHourlyRate(25.0);

        MachineResponse response = machineService.createMachine(request);

        assertNotNull(response.getQrToken());
        assertTrue(response.getQrToken().startsWith("QR-"));
        assertEquals("Test Washer", response.getMachineName());
    }

    @Test
    void createMachine_DefaultStatusIsAvailable() {
        MachineRequest request = new MachineRequest();
        request.setMachineName("Test Dryer");
        request.setMachineType(Machine.MachineType.DRYER);
        request.setHourlyRate(30.0);

        MachineResponse response = machineService.createMachine(request);

        assertEquals(Machine.MachineStatus.AVAILABLE, response.getStatus());
    }

    @Test
    void deleteMachine_NonExistentId_ThrowsException() {
        assertThrows(RuntimeException.class, () ->
            machineService.deleteMachine(99999L));
    }
}