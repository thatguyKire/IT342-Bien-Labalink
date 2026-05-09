package edu.cit.bien.labalink.auth;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@RestController
@RequestMapping("/api/oauth2")
@CrossOrigin(origins = "http://localhost:5173")
public class OAuth2Controller {

    // Web attendant Google login
    @GetMapping("/authorize/attendant")
    public void authorizeAttendant(
            HttpServletResponse response)
            throws IOException {
        response.sendRedirect(
            "/oauth2/authorization/google"
            + "?state=ATTENDANT"
        );
    }

    // Mobile customer Google login
    @GetMapping("/authorize/customer")
    public void authorizeCustomer(
            HttpServletResponse response)
            throws IOException {
        response.sendRedirect(
            "/oauth2/authorization/google"
            + "?state=CUSTOMER"
        );
    }
}