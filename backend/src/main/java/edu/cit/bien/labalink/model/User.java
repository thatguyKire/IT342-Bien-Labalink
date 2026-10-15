package edu.cit.bien.labalink.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.CUSTOMER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal walletBalance = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean isActive = true;

    // Incremented on logout / password change to revoke every issued JWT.
    @Column(nullable = false)
    private Integer tokenVersion = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    private LocalDateTime updatedAt;

    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public enum Role {
        CUSTOMER, ATTENDANT
    }

    public enum AuthProvider {
        LOCAL, GOOGLE
    }
}
