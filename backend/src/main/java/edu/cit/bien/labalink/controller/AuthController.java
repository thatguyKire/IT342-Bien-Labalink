package edu.cit.bien.labalink.controller;

import edu.cit.bien.labalink.dto.ApiResponse;
import edu.cit.bien.labalink.dto.LoginRequest;
import edu.cit.bien.labalink.dto.LoginResponse;
import edu.cit.bien.labalink.dto.RegisterRequest;
import edu.cit.bien.labalink.model.User;
import edu.cit.bien.labalink.repository.UserRepository;
import edu.cit.bien.labalink.security.JwtProvider;
import edu.cit.bien.labalink.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            authService.register(request);
            return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse(true, 
                    "Registration successful"));
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, 
                    "Invalid credentials"));
        }
    }

    @PostMapping("/google-login")
public ResponseEntity<?> googleLogin(
        @RequestBody Map<String, String> request) {
    try {
        String email = request.get("email");
        String name = request.get("name");
        String roleStr = request.get("role");

        User.Role role = "ATTENDANT"
            .equals(roleStr)
            ? User.Role.ATTENDANT
            : User.Role.CUSTOMER;

        // Find or create user
        User user = userRepository
            .findByEmail(email)
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setUsername(name);
                newUser.setPasswordHash(
                    "GOOGLE_AUTH_NO_PASSWORD");
                newUser.setRole(role);
                return userRepository.save(newUser);
            });

        String token = jwtProvider
            .generateToken(user.getEmail());

        return ResponseEntity.ok(
            new LoginResponse(
                token,
                "Bearer",
                user.getRole().name(),
                user.getEmail(),
                user.getUsername(),
                user.getId()
            ));
    } catch (Exception e) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiResponse(false,
                "Google login failed"));
    }
}

    
}


