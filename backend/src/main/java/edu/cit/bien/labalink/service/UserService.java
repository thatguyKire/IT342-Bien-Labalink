package edu.cit.bien.labalink.service;

import edu.cit.bien.labalink.dto.RegisterRequest;
import edu.cit.bien.labalink.dto.UserResponse;
import edu.cit.bien.labalink.exception.ApiException;
import edu.cit.bien.labalink.model.User;
import edu.cit.bien.labalink.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        return userRepository.findById(id)
            .map(UserResponse::fromEntity)
            .orElseThrow(() -> ApiException.notFound("User not found"));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAttendants() {
        return userRepository
            .findByRoleOrderByCreatedAtAsc(User.Role.ATTENDANT)
            .stream()
            .map(UserResponse::fromEntity)
            .toList();
    }

    @Transactional
    public UserResponse createAttendant(RegisterRequest request) {
        return UserResponse.fromEntity(authService.createLocalUser(
            request.getUsername(),
            request.getEmail(),
            request.getPassword(),
            User.Role.ATTENDANT));
    }
}
