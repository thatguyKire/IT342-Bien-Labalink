package edu.cit.bien.labalink.machine;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MachineRepository extends JpaRepository<Machine, Long> {
    
    // Your existing method
    boolean existsByQrToken(String qrToken);

    // 🟢 NEW METHOD: This fixes the red line in BookingService!
    Optional<Machine> findByQrToken(String qrToken);
}