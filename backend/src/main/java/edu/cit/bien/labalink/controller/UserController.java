package edu.cit.bien.labalink.controller;

import edu.cit.bien.labalink.dto.RegisterRequest;
import edu.cit.bien.labalink.dto.UserResponse;
import edu.cit.bien.labalink.security.UserPrincipal;
import edu.cit.bien.labalink.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.getUser(principal.id());
    }

    @GetMapping("/attendants")
    @PreAuthorize("hasRole('ATTENDANT')")
    public List<UserResponse> attendants() {
        return userService.getAttendants();
    }

    /** The only way (besides bootstrap config) to create an attendant. */
    @PostMapping("/attendants")
    @PreAuthorize("hasRole('ATTENDANT')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createAttendant(
            @Valid @RequestBody RegisterRequest request) {
        return userService.createAttendant(request);
    }
}
