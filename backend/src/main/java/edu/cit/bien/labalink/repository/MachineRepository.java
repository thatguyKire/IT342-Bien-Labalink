package edu.cit.bien.labalink.repository;

import edu.cit.bien.labalink.model.Machine;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MachineRepository
    extends JpaRepository<Machine, Long> {

    boolean existsByQrToken(String qrToken);

    List<Machine> findAllByOrderByIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Machine m where m.id = :id")
    Optional<Machine> findByIdForUpdate(@Param("id") Long id);
}
