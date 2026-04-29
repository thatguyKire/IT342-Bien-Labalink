package edu.cit.bien.labalink.repository;

import edu.cit.bien.labalink.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BookingRepository
    extends JpaRepository<Booking, Long> {

    List<Booking> findByStatus(
        Booking.BookingStatus status);

    List<Booking> findByUserEmail(String email);

    List<Booking> findByMachineId(Long machineId);
}