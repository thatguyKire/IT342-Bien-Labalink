package edu.cit.bien.labalink.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2SuccessHandler
        oauth2SuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(
                    SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**").permitAll()
                .requestMatchers(
                    "/api/machines/**").permitAll()
                .requestMatchers(
                    "/api/bookings/**").permitAll()
                .requestMatchers(
                    "/api/payments/**").permitAll()
                .requestMatchers(
                    "/api/oauth2/**").permitAll()
                .requestMatchers(
                    "/login/oauth2/**").permitAll()
                .requestMatchers(
                    "/oauth2/**").permitAll()
                .requestMatchers(
                    "/ws/**").permitAll()
                .requestMatchers(
                    "/error").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .successHandler(oauth2SuccessHandler)
                .failureUrl(
                    "http://localhost:5173/login"
                    + "?error=google_cancelled")
            );

        return http.build();
    }
}