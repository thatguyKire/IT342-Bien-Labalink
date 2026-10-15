package edu.cit.bien.labalink.repository;

import edu.cit.bien.labalink.model.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository
    extends JpaRepository<Booking, Long> {

    @EntityGraph(attributePaths = {"user", "machine"})
    List<Booking> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"user", "machine"})
    List<Booking> findByStatusOrderByCreatedAtDesc(
        Booking.BookingStatus status);

    @EntityGraph(attributePaths = {"user", "machine"})
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByStatus(Booking.BookingStatus status);

    boolean existsByMachineId(Long machineId);

    @Query("select coalesce(sum(b.totalPrice), 0) from Booking b "
        + "where b.status = edu.cit.bien.labalink.model"
        + ".Booking.BookingStatus.COMPLETED")
    BigDecimal sumCompletedRevenue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);
}
