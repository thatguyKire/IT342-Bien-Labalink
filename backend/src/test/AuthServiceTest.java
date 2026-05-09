package edu.cit.bien.labalink.auth;

import edu.cit.bien.labalink.dto.RegisterRequest;
import edu.cit.bien.labalink.dto.LoginRequest;
import edu.cit.bien.labalink.dto.LoginResponse;
import edu.cit.bien.labalink.repository.UserRepository;
import edu.cit.bien.labalink.service.AuthService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registerUser_Success() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("Test User");
        request.setEmail("test@labalink.com");
        request.setPassword("password123");

        authService.register(request);

        assertTrue(userRepository.existsByEmail("test@labalink.com"));
    }

    @Test
    void registerUser_DuplicateEmail_ThrowsException() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("Test User");
        request.setEmail("duplicate@labalink.com");
        request.setPassword("password123");

        authService.register(request);

        assertThrows(RuntimeException.class, () ->
            authService.register(request));
    }

    @Test
    void loginUser_ValidCredentials_ReturnsToken() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("Login Test");
        registerRequest.setEmail("login@labalink.com");
        registerRequest.setPassword("password123");
        authService.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("login@labalink.com");
        loginRequest.setPassword("password123");

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response.getAccessToken());
        assertEquals("CUSTOMER", response.getRole());
    }

    @Test
    void loginUser_WrongPassword_ThrowsException() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("Wrong Pass");
        registerRequest.setEmail("wrong@labalink.com");
        registerRequest.setPassword("password123");
        authService.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("wrong@labalink.com");
        loginRequest.setPassword("wrongpassword");

        assertThrows(RuntimeException.class, () ->
            authService.login(loginRequest));
    }
}