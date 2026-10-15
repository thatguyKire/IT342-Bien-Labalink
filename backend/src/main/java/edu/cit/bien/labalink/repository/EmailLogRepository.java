package edu.cit.bien.labalink.repository;

import edu.cit.bien.labalink.model.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailLogRepository
    extends JpaRepository<EmailLog, Long> {
}
