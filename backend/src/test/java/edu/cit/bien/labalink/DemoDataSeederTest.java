package edu.cit.bien.labalink;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cit.bien.labalink.config.AppProperties;
import edu.cit.bien.labalink.config.DemoDataSeeder;
import edu.cit.bien.labalink.support.FakePaymentGateway;
import edu.cit.bien.labalink.support.TestInfrastructure;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Boots with DEMO_DATA=true on its own empty database. */
@SpringBootTest(properties = {
    "app.demo.enabled=true",
    "app.demo.password=" + DemoDataSeederTest.DEMO_PASSWORD})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemoDataSeederTest {

    static final String DEMO_PASSWORD = "Demo-pass-1";

    @MockitoBean JavaMailSender mailSender;
    @Autowired DemoDataSeeder seeder;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @TestConfiguration(proxyBeanMethods = false)
    static class EmptyDatabase {

        @Bean
        @Primary
        DataSource dataSource() throws Exception {
            var postgres = TestInfrastructure.postgres();
            try (Connection c = postgres.getPostgresDatabase().getConnection();
                 Statement s = c.createStatement()) {
                s.execute("DROP DATABASE IF EXISTS demo_seed");
                s.execute("CREATE DATABASE demo_seed");
            }
            return postgres.getDatabase("postgres", "demo_seed");
        }

        @Bean
        @Primary
        FakePaymentGateway fakePaymentGateway() {
            return new FakePaymentGateway();
        }
    }

    @Test
    void seedsMachinesInEveryState() {
        assertThat(count("select count(*) from machines")).isEqualTo(8);
        assertThat(count("select count(*) from users where role='CUSTOMER'"))
            .isEqualTo(3);
        assertThat(count(
                "select count(*) from machines where status='OUT_OF_ORDER'"))
            .isEqualTo(1);
        assertThat(count(
                "select count(distinct qr_token) from machines "
                + "where qr_token like 'LL-%'"))
            .isEqualTo(8);
    }

    @Test
    void everyRunningMachineHasExactlyOneOpenSession() {
        String openSessions = "(select count(*) from bookings b "
            + "where b.machine_id = m.id and b.status in ('PENDING','ACTIVE'))";
        assertThat(count("select count(*) from machines m "
                + "where m.status='RUNNING'"))
            .isEqualTo(3);
        assertThat(count("select count(*) from machines m where "
                + "(m.status='RUNNING') <> (" + openSessions + " = 1)"))
            .isZero();
    }

    @Test
    void walletsMatchTopUpsMinusSessions() {
        assertThat(count("""
                select count(*) from users u where u.wallet_balance <>
                  coalesce((select sum(p.amount) from payments p
                            where p.user_id = u.id
                              and p.status = 'SUCCESS'), 0)
                - coalesce((select sum(b.total_price) from bookings b
                            where b.user_id = u.id
                              and b.status <> 'CANCELLED'), 0)
                """))
            .isZero();
    }

    @Test
    void primaryCustomerCanSignInAndSeeTheirWallet() throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                    "email", DemoDataSeeder.PRIMARY_CUSTOMER_EMAIL,
                    "password", DEMO_PASSWORD))))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String token = json.readTree(body).get("accessToken").asText();

        mvc.perform(get("/api/payments/wallet")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.walletBalance").value(472.50));
        mvc.perform(get("/api/bookings/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    void runningAgainAddsNothing() {
        long bookings = count("select count(*) from bookings");
        long payments = count("select count(*) from payments");

        seeder.run(null);

        assertThat(count("select count(*) from machines")).isEqualTo(8);
        assertThat(count("select count(*) from bookings")).isEqualTo(bookings);
        assertThat(count("select count(*) from payments")).isEqualTo(payments);
        assertThat(jdbc.queryForObject(
                "select wallet_balance from users where email = ?",
                BigDecimal.class, DemoDataSeeder.PRIMARY_CUSTOMER_EMAIL))
            .isEqualByComparingTo("472.50");
    }

    @Test
    void demoModeRequiresAPassword() {
        assertThatThrownBy(() -> new AppProperties.Demo(true, ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("DEMO_PASSWORD");
        assertThatThrownBy(() -> new AppProperties.Demo(true, "short"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> new AppProperties.Demo(false, ""))
            .doesNotThrowAnyException();
        assertThatCode(() -> new AppProperties.Demo(true, DEMO_PASSWORD))
            .doesNotThrowAnyException();
    }

    private long count(String sql) {
        return jdbc.queryForObject(sql, Long.class);
    }
}
