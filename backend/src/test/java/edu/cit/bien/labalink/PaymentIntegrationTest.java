package edu.cit.bien.labalink;

import com.fasterxml.jackson.databind.JsonNode;
import edu.cit.bien.labalink.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentIntegrationTest extends IntegrationTestBase {

    @Test
    void topUpLimitsAreEnforced() throws Exception {
        String customer = registerCustomer(uniqueEmail("limits"));
        postAs(customer, "/api/payments/checkout", Map.of("amount", 10))
            .andExpect(status().isBadRequest());
        postAs(customer, "/api/payments/checkout", Map.of("amount", 999999))
            .andExpect(status().isBadRequest());
        postAs(customer, "/api/payments/checkout", Map.of("amount", -100))
            .andExpect(status().isBadRequest());
    }

    @Test
    void walletIsCreditedOnlyOnceAndOnlyWhenPaid() throws Exception {
        String email = uniqueEmail("topup");
        String customer = registerCustomer(email);

        JsonNode payment = read(postAs(customer, "/api/payments/checkout",
                Map.of("amount", 150))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.checkoutUrl").exists()));
        long paymentId = payment.get("id").asLong();
        String sessionId = paymentGateway.lastSessionId();

        // Not paid yet: syncing changes nothing.
        postAs(customer, "/api/payments/" + paymentId + "/sync", null)
            .andExpect(jsonPath("$.status").value("PENDING"));
        assertThat(balanceOf(email)).isEqualByComparingTo("0");

        paymentGateway.markPaid(sessionId);
        postAs(customer, "/api/payments/" + paymentId + "/sync", null)
            .andExpect(jsonPath("$.status").value("SUCCESS"));
        assertThat(balanceOf(email)).isEqualByComparingTo("150.00");

        // Replays (sync again, webhook again) never add money twice.
        postAs(customer, "/api/payments/" + paymentId + "/sync", null)
            .andExpect(status().isOk());
        mvc.perform(post("/api/payments/webhook")
                .header("Stripe-Signature", "valid-signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sessionId))
            .andExpect(status().isOk());
        assertThat(balanceOf(email)).isEqualByComparingTo("150.00");
    }

    @Test
    void webhookCreditsWallet() throws Exception {
        String email = uniqueEmail("webhook");
        String customer = registerCustomer(email);
        postAs(customer, "/api/payments/checkout", Map.of("amount", 200))
            .andExpect(status().isCreated());
        String sessionId = paymentGateway.lastSessionId();
        paymentGateway.markPaid(sessionId);

        mvc.perform(post("/api/payments/webhook")
                .header("Stripe-Signature", "valid-signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sessionId))
            .andExpect(status().isOk());
        assertThat(balanceOf(email)).isEqualByComparingTo("200.00");
    }

    @Test
    void forgedWebhooksAreRejected() throws Exception {
        mvc.perform(post("/api/payments/webhook")
                .header("Stripe-Signature", "forged")
                .contentType(MediaType.APPLICATION_JSON)
                .content("cs_test_1"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("cs_test_1"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void amountMismatchIsNeverCredited() throws Exception {
        String email = uniqueEmail("mismatch");
        String customer = registerCustomer(email);
        long paymentId = read(postAs(customer, "/api/payments/checkout",
                Map.of("amount", 500))
            .andExpect(status().isCreated())).get("id").asLong();

        paymentGateway.markPaid(paymentGateway.lastSessionId(), 100);
        postAs(customer, "/api/payments/" + paymentId + "/sync", null)
            .andExpect(jsonPath("$.status").value("FAILED"));
        assertThat(balanceOf(email)).isEqualByComparingTo("0");
    }

    @Test
    void customersCannotTouchOtherPeoplesPayments() throws Exception {
        String owner = registerCustomer(uniqueEmail("payowner"));
        long paymentId = read(postAs(owner, "/api/payments/checkout",
                Map.of("amount", 100))
            .andExpect(status().isCreated())).get("id").asLong();

        String stranger = registerCustomer(uniqueEmail("paystranger"));
        postAs(stranger, "/api/payments/" + paymentId + "/sync", null)
            .andExpect(status().isNotFound());
        getAs(stranger, "/api/payments/me")
            .andExpect(jsonPath("$.length()").value(0));
    }
}
