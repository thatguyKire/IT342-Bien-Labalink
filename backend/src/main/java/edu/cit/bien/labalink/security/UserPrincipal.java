package edu.cit.bien.labalink.security;

import edu.cit.bien.labalink.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.io.Serializable;
import java.security.Principal;
import java.util.List;

/**
 * Authenticated caller, resolved from the database on every request so that
 * role changes and deactivation take effect immediately.
 * getName() returns the user id, which is also the STOMP user-destination key.
 */
public record UserPrincipal(
        Long id,
        String email,
        String username,
        User.Role role) implements Principal, Serializable {

    public static UserPrincipal from(User user) {
        return new UserPrincipal(
            user.getId(),
            user.getEmail(),
            user.getUsername(),
            user.getRole());
    }

    @Override
    public String getName() {
        return String.valueOf(id);
    }

    public boolean isAttendant() {
        return role == User.Role.ATTENDANT;
    }

    public List<GrantedAuthority> authorities() {
        return List.of(
            new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
