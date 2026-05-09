package edu.cit.bien.labalink.config;

import edu.cit.bien.labalink.auth.User;
import edu.cit.bien.labalink.auth.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler
    extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException {

        OAuth2User oAuth2User =
            (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User
            .getAttribute("email");
        String name = oAuth2User
            .getAttribute("name");

        // Null check for email
        if (email == null || email.isEmpty()) {
            response.sendRedirect(
                "http://localhost:5173/login"
                + "?error=no_email");
            return;
        }

        // Null check for name
        if (name == null) {
            name = email.split("@")[0];
        }

        // Read role hint from state parameter
        String stateParam = request
            .getParameter("state");
        User.Role assignedRole = User.Role.CUSTOMER;

        if (stateParam != null &&
            stateParam.contains("ATTENDANT")) {
            assignedRole = User.Role.ATTENDANT;
        }

        // Find existing user or create new one
        Optional<User> existingUser =
            userRepository.findByEmail(email);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            user = new User();
            user.setEmail(email);
            user.setUsername(name);
            user.setPasswordHash(
                "GOOGLE_AUTH_NO_PASSWORD");
            user.setRole(assignedRole);
            userRepository.save(user);
        }

        // Generate JWT
        String token = jwtProvider
            .generateToken(user.getEmail());

        // Build redirect URL with encoded params
        String redirectUrl =
            "http://localhost:5173/oauth2/callback"
            + "?token=" + token
            + "&username=" + URLEncoder.encode(
                user.getUsername(),
                StandardCharsets.UTF_8)
            + "&role=" + user.getRole().name()
            + "&email=" + URLEncoder.encode(
                user.getEmail(),
                StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}