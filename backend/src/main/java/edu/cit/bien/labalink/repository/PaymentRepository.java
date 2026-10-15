package edu.cit.bien.labalink.repository;

import edu.cit.bien.labalink.model.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository
    extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<Payment> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"user"})
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Payment> findByStripeCheckoutSessionId(String sessionId);
}
