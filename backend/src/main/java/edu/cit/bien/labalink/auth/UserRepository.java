package edu.cit.bien.labalink.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 🟢 Used by UserDetailsService in SecurityConfig to verify JWT tokens
    Optional<User> findByEmail(String email);

    // 🟢 Used in your Register flow to prevent duplicate accounts
    boolean existsByEmail(String email);

    // 🟡 Optional: If your 'User' entity has a 'username' field and you use it for login
    // Optional<User> findByUsername(String username);
}