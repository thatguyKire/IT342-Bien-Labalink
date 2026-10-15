package edu.cit.bien.labalink.security;

import edu.cit.bien.labalink.config.AppProperties;
import edu.cit.bien.labalink.exception.ApiException;
import edu.cit.bien.labalink.model.User;
import edu.cit.bien.labalink.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

/**
 * Completes the web (attendant dashboard) Google login.
 * The role is never taken from the request: new Google accounts are always
 * CUSTOMER, and only existing ATTENDANT accounts can use the dashboard.
 */
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final OAuth2CodeStore codeStore;
    private final AppProperties properties;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        Object emailVerified = oAuth2User.getAttribute("email_verified");

        // The HTTP session only existed to hold OAuth2 state; drop it so it
        // can never be used as an ambient (CSRF-able) credential.
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        String loginUrl = properties.frontendUrl() + "/login";
        if (email == null || email.isBlank()
                || !Boolean.parseBoolean(String.valueOf(emailVerified))) {
            response.sendRedirect(loginUrl + "?error=google_unverified");
            return;
        }

        User user;
        try {
            user = authService.findOrCreateGoogleUser(email, name);
        } catch (ApiException e) {
            response.sendRedirect(loginUrl + "?error=account_disabled");
            return;
        }

        String code = codeStore.issue(user.getId());
        // Fragment (#) is never sent to servers or leaked via Referer.
        response.sendRedirect(
            properties.frontendUrl() + "/oauth2/callback#code=" + code);
    }
}
