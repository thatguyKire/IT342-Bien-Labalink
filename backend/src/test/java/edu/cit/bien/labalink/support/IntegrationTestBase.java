package edu.cit.bien.labalink.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cit.bien.labalink.model.User;
import edu.cit.bien.labalink.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestInfrastructure.class)
public abstract class IntegrationTestBase {

    protected static final String PASSWORD = "Passw0rd!23";

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected UserRepository userRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected FakePaymentGateway paymentGateway;

    @MockitoBean protected JavaMailSender mailSender;

    protected static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8)
            + "@test.local";
    }

    protected String registerCustomer(String email) throws Exception {
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of(
                    "username", "Customer",
                    "email", email,
                    "password", PASSWORD))))
            .andExpect(status().isCreated());
        return login(email, PASSWORD);
    }

    protected String createAttendant() throws Exception {
        String email = uniqueEmail("attendant");
        User user = new User();
        user.setUsername("Attendant");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(User.Role.ATTENDANT);
        userRepository.save(user);
        return login(email, PASSWORD);
    }

    protected String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of(
                    "email", email, "password", password))))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("accessToken").asText();
    }

    protected void setBalance(String email, String amount) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setWalletBalance(new BigDecimal(amount));
        userRepository.save(user);
    }

    protected BigDecimal balanceOf(String email) {
        return userRepository.findByEmail(email).orElseThrow()
            .getWalletBalance();
    }

    protected JsonNode createMachine(String attendantToken, String rate)
            throws Exception {
        String body = mvc.perform(post("/api/machines")
                .header("Authorization", bearer(attendantToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(Map.of(
                    "machineName", "Washer " + UUID.randomUUID()
                        .toString().substring(0, 4),
                    "machineType", "WASHER",
                    "hourlyRate", new BigDecimal(rate)))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    protected ResultActions getAs(String token, String url) throws Exception {
        return mvc.perform(get(url).header("Authorization", bearer(token)));
    }

    protected ResultActions postAs(String token, String url, Object body)
            throws Exception {
        return mvc.perform(post(url)
            .header("Authorization", bearer(token))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body == null ? "{}" : toJson(body)));
    }

    protected JsonNode read(ResultActions result) throws Exception {
        return json.readTree(
            result.andReturn().getResponse().getContentAsString());
    }

    protected String toJson(Object value) throws Exception {
        return json.writeValueAsString(value);
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }
}
