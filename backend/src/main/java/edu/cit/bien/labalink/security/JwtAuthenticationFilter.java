package edu.cit.bien.labalink.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/**
 * Authenticates requests carrying "Authorization: Bearer <jwt>".
 * Invalid or missing tokens leave the request anonymous; the security
 * rules then answer 401 for protected endpoints.
 * Not a @Component on purpose, so it is not also registered as a
 * plain servlet filter outside the security chain.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtAuthenticationService authenticationService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String token = JwtAuthenticationService.extractBearer(
            request.getHeader(HttpHeaders.AUTHORIZATION));

        if (token != null) {
            authenticationService.authenticate(token)
                .ifPresent(authentication -> {
                    authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                            .buildDetails(request));
                    SecurityContext context =
                        SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(authentication);
                    SecurityContextHolder.setContext(context);
                });
        }

        chain.doFilter(request, response);
    }
}
