package edu.cit.bien.labalink.email;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailLogRepository
    extends JpaRepository<EmailLog, Long> {

    List<EmailLog> findByToEmail(String email);
    List<EmailLog> findByEmailType(
        EmailLog.EmailType emailType);
}