package edu.cit.bien.labalink.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation
    .web.builders.HttpSecurity;
import org.springframework.security.config.annotation
    .web.configuration.EnableWebSecurity;
import org.springframework.security.config.http
    .SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt
    .BCryptPasswordEncoder;
import org.springframework.security.crypto.password
    .PasswordEncoder;
import org.springframework.security.web
    .SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable()) // Disable CSRF for REST APIs
        .authorizeHttpRequests(auth -> auth
            // ALLOW your auth endpoints (adjust the prefix if your controllers don't use /api/v1)
            .requestMatchers("/api/v1/auth/**").permitAll() 
            .requestMatchers("/auth/**").permitAll() // Just in case you didn't add the /api/v1 prefix
            .requestMatchers("/error").permitAll()   // IMPORTANT: Allows Spring to show actual error messages instead of Access Denied
            .anyRequest().authenticated()            // Secure everything else
        )
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        
    return http.build();
}
}