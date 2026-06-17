package edu.cit.bien.labalink.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository
    extends JpaRepository<Payment, Long> {

    List<Payment> findByUserEmail(String email);

    List<Payment> findByStatus(
        Payment.PaymentStatus status);
}