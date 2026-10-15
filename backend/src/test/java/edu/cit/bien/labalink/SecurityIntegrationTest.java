package edu.cit.bien.labalink;

import com.fasterxml.jackson.databind.JsonNode;
import edu.cit.bien.labalink.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIntegrationTest extends IntegrationTestBase {

    @Test
    void protectedEndpointsRequireAuthentication() throws Exception {
        for (String url : new String[] {
                "/api/machines", "/api/bookings", "/api/bookings/me",
                "/api/payments", "/api/payments/wallet", "/api/users/me"}) {
            mvc.perform(get(url)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void legacyTrustTheClientGoogleEndpointIsGone() throws Exception {
        // The old endpoint issued a JWT for any email in the request body.
        mvc.perform(post("/api/auth/google-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"victim@test.local\",\"role\":\"ATTENDANT\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void googleEndpointRejectsUnverifiableTokens() throws Exception {
        mvc.perform(post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idToken\":\"not-a-real-google-token\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void oauth2ExchangeRejectsUnknownCodes() throws Exception {
        mvc.perform(post("/api/auth/oauth2/exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"guessed-code\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationCannotSelfAssignAttendantRole() throws Exception {
        String email = uniqueEmail("sneaky");
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of(
                    "username", "Sneaky",
                    "email", email,
                    "password", PASSWORD,
                    "role", "ATTENDANT"))))
            .andExpect(status().isCreated());

        String token = login(email, PASSWORD);
        getAs(token, "/api/users/me")
            .andExpect(jsonPath("$.role").value("CUSTOMER"));
        getAs(token, "/api/bookings").andExpect(status().isForbidden());
    }

    @Test
    void registrationEnforcesPasswordPolicy() throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of(
                    "username", "Weak",
                    "email", uniqueEmail("weak"),
                    "password", "password"))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void customersCannotUseAttendantEndpoints() throws Exception {
        String customer = registerCustomer(uniqueEmail("customer"));

        postAs(customer, "/api/machines", Map.of(
                "machineName", "Hack", "machineType", "WASHER",
                "hourlyRate", 1))
            .andExpect(status().isForbidden());
        getAs(customer, "/api/payments").andExpect(status().isForbidden());
        getAs(customer, "/api/bookings/stats")
            .andExpect(status().isForbidden());
        postAs(customer, "/api/users/attendants", Map.of(
                "username", "Evil", "email", uniqueEmail("evil"),
                "password", PASSWORD))
            .andExpect(status().isForbidden());
    }

    @Test
    void qrTokensAreOnlyVisibleToAttendants() throws Exception {
        String attendant = createAttendant();
        JsonNode machine = createMachine(attendant, "30.00");
        assertThat(machine.get("qrToken").asText()).startsWith("LL-");

        String customer = registerCustomer(uniqueEmail("viewer"));
        getAs(customer, "/api/machines/" + machine.get("id").asLong())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.qrToken").doesNotExist());
    }

    @Test
    void tamperedAndRevokedTokensAreRejected() throws Exception {
        String token = registerCustomer(uniqueEmail("revoke"));
        getAs(token, "/api/users/me").andExpect(status().isOk());

        String tampered = token.substring(0, token.length() - 2) + "xx";
        getAs(tampered, "/api/users/me").andExpect(status().isUnauthorized());

        postAs(token, "/api/auth/logout", null).andExpect(status().isOk());
        getAs(token, "/api/users/me").andExpect(status().isUnauthorized());
    }

    @Test
    void repeatedFailedLoginsLockTheAccount() throws Exception {
        String email = uniqueEmail("brute");
        registerCustomer(email);

        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(Map.of(
                        "email", email, "password", "Wrong-pass1"))))
                .andExpect(status().isUnauthorized());
        }
        // Even the correct password is refused while locked.
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of("email", email, "password", PASSWORD))))
            .andExpect(status().isTooManyRequests());
    }

    @Test
    void unknownEmailAndWrongPasswordLookIdentical() throws Exception {
        String email = uniqueEmail("known");
        registerCustomer(email);

        String wrongPassword = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of(
                    "email", email, "password", "Wrong-pass1"))))
            .andExpect(status().isUnauthorized())
            .andReturn().getResponse().getContentAsString();
        String unknownEmail = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of(
                    "email", uniqueEmail("nobody"),
                    "password", "Wrong-pass1"))))
            .andExpect(status().isUnauthorized())
            .andReturn().getResponse().getContentAsString();

        assertThat(wrongPassword).isEqualTo(unknownEmail);
    }

    @Test
    void responsesCarrySecurityHeaders() throws Exception {
        mvc.perform(get("/api/machines"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("X-Frame-Options", "DENY"))
            .andExpect(header().exists("Content-Security-Policy"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"));
    }

    @Test
    void corsOnlyAllowsConfiguredOrigins() throws Exception {
        mvc.perform(options("/api/machines")
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isForbidden());

        mvc.perform(options("/api/bookings/1/status")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "PATCH"))
            .andExpect(status().isOk())
            .andExpect(header().string(
                "Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void errorsNeverLeakInternals() throws Exception {
        String customer = registerCustomer(uniqueEmail("errors"));
        getAs(customer, "/api/bookings/not-a-number")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Malformed request"))
            .andExpect(jsonPath("$.trace").doesNotExist());
    }
}
