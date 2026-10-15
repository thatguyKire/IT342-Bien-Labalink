package edu.cit.bien.labalink.service;

import edu.cit.bien.labalink.dto.LoginRequest;
import edu.cit.bien.labalink.dto.LoginResponse;
import edu.cit.bien.labalink.dto.RegisterRequest;
import edu.cit.bien.labalink.exception.ApiException;
import edu.cit.bien.labalink.model.User;
import edu.cit.bien.labalink.repository.UserRepository;
import edu.cit.bien.labalink.security.GoogleIdTokenVerifier;
import edu.cit.bien.labalink.security.JwtProvider;
import edu.cit.bien.labalink.security.LoginAttemptService;
import edu.cit.bien.labalink.security.OAuth2CodeStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    // BCrypt only looks at the first 72 bytes; longer input is rejected
    // rather than silently truncated.
    private static final int BCRYPT_MAX_BYTES = 72;
    private static final String INVALID_CREDENTIALS =
        "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final EmailService emailService;
    private final LoginAttemptService loginAttempts;
    private final GoogleIdTokenVerifier googleVerifier;
    private final OAuth2CodeStore oauth2CodeStore;
    private final SecureRandom random = new SecureRandom();

    // Compared against when the account does not exist, so response time
    // does not reveal which emails are registered.
    private String dummyHash;

    @PostConstruct
    void init() {
        dummyHash = passwordEncoder.encode(randomSecret());
    }

    public static String normalizeEmail(String email) {
        return email == null ? null
            : email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public void register(RegisterRequest request) {
        createLocalUser(
            request.getUsername(),
            request.getEmail(),
            request.getPassword(),
            User.Role.CUSTOMER);
    }

    @Transactional
    public User createLocalUser(
            String username,
            String rawEmail,
            String password,
            User.Role role) {
        String email = normalizeEmail(rawEmail);
        if (password == null
                || password.getBytes(StandardCharsets.UTF_8).length
                    > BCRYPT_MAX_BYTES) {
            throw ApiException.badRequest("Password is too long");
        }
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict(
                "An account with this email already exists");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setAuthProvider(User.AuthProvider.LOCAL);
        User saved = userRepository.save(user);

        AfterCommit.run(() -> emailService.sendWelcomeEmail(
            saved.getEmail(), saved.getUsername()));
        return saved;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (loginAttempts.isLocked(email)) {
            throw ApiException.tooManyRequests(
                "Too many failed attempts. Try again in "
                + loginAttempts.lockoutMinutes() + " minutes.");
        }

        Optional<User> found = userRepository.findByEmail(email)
            .filter(u -> u.getAuthProvider() == User.AuthProvider.LOCAL);
        String hash = found.map(User::getPasswordHash).orElse(dummyHash);
        String password = request.getPassword();

        boolean passwordOk =
            password.getBytes(StandardCharsets.UTF_8).length
                <= BCRYPT_MAX_BYTES
            && passwordEncoder.matches(password, hash);

        if (found.isEmpty() || !passwordOk) {
            loginAttempts.recordFailure(email);
            throw ApiException.unauthorized(INVALID_CREDENTIALS);
        }

        User user = found.get();
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw ApiException.forbidden("This account is disabled");
        }
        loginAttempts.recordSuccess(email);
        return toLoginResponse(user);
    }

    @Transactional
    public LoginResponse loginWithGoogleIdToken(String idToken) {
        GoogleIdTokenVerifier.GoogleUser googleUser =
            googleVerifier.verify(idToken);
        return toLoginResponse(findOrCreateGoogleUser(
            googleUser.email(), googleUser.name()));
    }

    @Transactional(readOnly = true)
    public LoginResponse exchangeOAuth2Code(String code) {
        Long userId = oauth2CodeStore.consume(code)
            .orElseThrow(() -> ApiException.unauthorized(
                "Sign-in link expired. Please sign in again."));
        User user = userRepository.findById(userId)
            .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
            .orElseThrow(() -> ApiException.unauthorized(
                "Sign-in failed. Please try again."));
        return toLoginResponse(user);
    }

    /**
     * Google has verified the email, so an existing account with that email
     * is signed in; otherwise a new CUSTOMER account is created. The role is
     * never taken from the client.
     */
    @Transactional
    public User findOrCreateGoogleUser(String rawEmail, String name) {
        String email = normalizeEmail(rawEmail);
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            if (!Boolean.TRUE.equals(existing.get().getIsActive())) {
                throw ApiException.forbidden("This account is disabled");
            }
            return existing.get();
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(sanitizeUsername(name, email));
        // Unusable random password: Google accounts cannot log in with one.
        user.setPasswordHash(passwordEncoder.encode(randomSecret()));
        user.setRole(User.Role.CUSTOMER);
        user.setAuthProvider(User.AuthProvider.GOOGLE);
        User saved = userRepository.save(user);

        AfterCommit.run(() -> emailService.sendWelcomeEmail(
            saved.getEmail(), saved.getUsername()));
        return saved;
    }

    /** Revokes every token issued to the user (all devices). */
    @Transactional
    public void logout(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setTokenVersion(user.getTokenVersion() + 1);
            userRepository.save(user);
        });
    }

    private LoginResponse toLoginResponse(User user) {
        return new LoginResponse(
            jwtProvider.generateToken(user),
            "Bearer",
            jwtProvider.getExpirationSeconds(),
            user.getRole().name(),
            user.getEmail(),
            user.getUsername(),
            user.getId());
    }

    private static String sanitizeUsername(String name, String email) {
        String candidate = name == null ? ""
            : name.replaceAll("[^\\p{L}\\p{N} ._-]", "").trim();
        if (candidate.length() < 3) {
            candidate = email.substring(0, email.indexOf('@'))
                .replaceAll("[^\\p{L}\\p{N}._-]", "");
        }
        if (candidate.length() < 3) {
            candidate = "LabaLink user";
        }
        return candidate.length() > 50
            ? candidate.substring(0, 50).trim()
            : candidate;
    }

    private String randomSecret() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
