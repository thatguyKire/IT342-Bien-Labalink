package edu.cit.bien.labalink.config;

import edu.cit.bien.labalink.model.User;
import edu.cit.bien.labalink.repository.UserRepository;
import edu.cit.bien.labalink.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first attendant from BOOTSTRAP_ATTENDANT_* settings when no
 * attendant exists yet. Attendants can then add colleagues from the
 * dashboard. Public sign-up can never create an attendant.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final AppProperties properties;
    private final UserRepository userRepository;
    private final AuthService authService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.Bootstrap bootstrap = properties.bootstrap();
        if (bootstrap == null
                || isBlank(bootstrap.attendantEmail())
                || isBlank(bootstrap.attendantPassword())
                || userRepository.existsByRole(User.Role.ATTENDANT)) {
            return;
        }

        String email = AuthService.normalizeEmail(
            bootstrap.attendantEmail());
        userRepository.findByEmail(email).ifPresentOrElse(
            existing -> {
                existing.setRole(User.Role.ATTENDANT);
                existing.setTokenVersion(existing.getTokenVersion() + 1);
                userRepository.save(existing);
                log.info("Promoted bootstrap user to ATTENDANT");
            },
            () -> {
                authService.createLocalUser(
                    bootstrap.attendantUsername(),
                    email,
                    bootstrap.attendantPassword(),
                    User.Role.ATTENDANT);
                log.info("Created bootstrap ATTENDANT account");
            });
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
