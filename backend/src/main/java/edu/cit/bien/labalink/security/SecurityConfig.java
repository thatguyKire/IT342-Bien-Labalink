package edu.cit.bien.labalink.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cit.bien.labalink.config.AppProperties;
import edu.cit.bien.labalink.dto.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2SuccessHandler oauth2SuccessHandler;
    private final JwtAuthenticationService jwtAuthenticationService;
    private final AppProperties properties;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http)
            throws Exception {
        http
            // Stateless bearer-token API: no cookie carries authentication,
            // so there is nothing for a CSRF attack to ride on.
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            // A session is created only to hold OAuth2 "state" during the
            // Google redirect; authentication is never stored in it.
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .securityContext(context -> context
                .securityContextRepository(
                    new RequestAttributeSecurityContextRepository()))
            .requestCache(cache -> cache
                .requestCache(new NullRequestCache()))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(
                    "default-src 'none'; frame-ancestors 'none'; "
                    + "base-uri 'none'; form-action 'self'"))
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(referrer -> referrer.policy(
                    ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31_536_000))
                .addHeaderWriter(new StaticHeadersWriter(
                    "Permissions-Policy",
                    "camera=(), microphone=(), geolocation=()"))
                .addHeaderWriter(new StaticHeadersWriter(
                    "Cross-Origin-Opener-Policy", "same-origin")))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/google",
                    "/api/auth/oauth2/exchange").permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/api/payments/webhook").permitAll()
                .requestMatchers(
                    "/oauth2/authorization/**",
                    "/login/oauth2/code/**").permitAll()
                // The handshake is public; the STOMP CONNECT frame is
                // authenticated by WebSocketAuthInterceptor.
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll())
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oauth2SuccessHandler)
                .failureHandler((request, response, exception) ->
                    response.sendRedirect(properties.frontendUrl()
                        + "/login?error=google_cancelled")))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, e) ->
                    writeError(response, 401, "Authentication required"))
                .accessDeniedHandler((request, response, e) ->
                    writeError(response, 403, "Access denied")))
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtAuthenticationService),
                UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(
                new RateLimitFilter(
                    properties.rateLimit().authRequestsPerMinute(),
                    properties.rateLimit().apiRequestsPerMinute()),
                JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Explicit allow-list only. Never "*", never origin patterns.
        config.setAllowedOrigins(properties.cors().allowedOrigins());
        config.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
            "Authorization", "Content-Type"));
        // Bearer tokens, not cookies: credentials are not needed.
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/ws/**", config);
        return source;
    }

    private void writeError(
            HttpServletResponse response,
            int status,
            String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
            response.getOutputStream(),
            new ApiResponse(false, message));
    }
}
