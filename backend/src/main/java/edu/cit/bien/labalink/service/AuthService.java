package edu.cit.bien.labalink.service;

import edu.cit.bien.labalink.dto.LoginRequest;
import edu.cit.bien.labalink.dto.LoginResponse;
import edu.cit.bien.labalink.dto.RegisterRequest;
import edu.cit.bien.labalink.model.User;
import edu.cit.bien.labalink.repository.UserRepository;
import edu.cit.bien.labalink.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password
    .PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public void register(RegisterRequest request) {
        if (userRepository
                .existsByEmail(request.getEmail())) {
            throw new RuntimeException(
                "Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(
            passwordEncoder.encode(
                request.getPassword()));
        user.setRole(User.Role.CUSTOMER);

        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> 
                    new RuntimeException(
                        "Invalid credentials"));

        if (!passwordEncoder.matches(
                request.getPassword(), 
                user.getPasswordHash())) {
            throw new RuntimeException(
                "Invalid credentials");
        }

        String token = jwtProvider
            .generateToken(user.getEmail());

        return new LoginResponse(
                token,
                "Bearer",
                user.getRole().name(),
                user.getEmail(),
                user.getUsername()
        );
    }
}