package edu.cit.bien.labalink.repository;

import edu.cit.bien.labalink.model.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MachineRepository 
    extends JpaRepository<Machine, Long> {
    boolean existsByQrToken(String qrToken);
}