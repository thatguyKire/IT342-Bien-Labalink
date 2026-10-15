package edu.cit.bien.labalink.controller;

import edu.cit.bien.labalink.dto.ApiResponse;
import edu.cit.bien.labalink.dto.GoogleTokenRequest;
import edu.cit.bien.labalink.dto.LoginRequest;
import edu.cit.bien.labalink.dto.LoginResponse;
import edu.cit.bien.labalink.dto.OAuth2ExchangeRequest;
import edu.cit.bien.labalink.dto.RegisterRequest;
import edu.cit.bien.labalink.security.UserPrincipal;
import edu.cit.bien.labalink.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** Public sign-up; always creates a CUSTOMER. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return new ApiResponse(true, "Registration successful");
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** Android Google Sign-In: the server verifies the Google ID token. */
    @PostMapping("/google")
    public LoginResponse google(
            @Valid @RequestBody GoogleTokenRequest request) {
        return authService.loginWithGoogleIdToken(request.getIdToken());
    }

    /** Web Google login: swaps the one-time redirect code for a JWT. */
    @PostMapping("/oauth2/exchange")
    public LoginResponse exchange(
            @Valid @RequestBody OAuth2ExchangeRequest request) {
        return authService.exchangeOAuth2Code(request.getCode());
    }

    /** Revokes all of the caller's tokens. */
    @PostMapping("/logout")
    public ApiResponse logout(
            @AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.id());
        return new ApiResponse(true, "Logged out");
    }
}
