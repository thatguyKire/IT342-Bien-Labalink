package edu.cit.bien.labalink;

import com.fasterxml.jackson.databind.JsonNode;
import edu.cit.bien.labalink.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingIntegrationTest extends IntegrationTestBase {

    @Test
    void bookingRequiresTheRealQrCode() throws Exception {
        String attendant = createAttendant();
        JsonNode machine = createMachine(attendant, "30.00");
        String email = uniqueEmail("qr");
        String customer = registerCustomer(email);
        setBalance(email, "500.00");

        postAs(customer, "/api/bookings", Map.of(
                "machineId", machine.get("id").asLong(),
                "qrToken", "LL-guessed"))
            .andExpect(status().isBadRequest());
        assertThat(balanceOf(email)).isEqualByComparingTo("500.00");
    }

    @Test
    void bookingIsPricedByServerAndChargedToWallet() throws Exception {
        String attendant = createAttendant();
        JsonNode machine = createMachine(attendant, "30.00");
        String email = uniqueEmail("payer");
        String customer = registerCustomer(email);
        setBalance(email, "100.00");

        // Client tries to set its own price and user; both are ignored.
        JsonNode booking = read(postAs(customer, "/api/bookings", Map.of(
                "machineId", machine.get("id").asLong(),
                "qrToken", machine.get("qrToken").asText(),
                "durationMinutes", 90,
                "totalPrice", 0.01,
                "userId", 1))
            .andExpect(status().isCreated()));

        assertThat(booking.get("totalPrice").decimalValue())
            .isEqualByComparingTo("45.00");
        assertThat(booking.get("userEmail").asText()).isEqualTo(email);
        assertThat(balanceOf(email)).isEqualByComparingTo("55.00");

        // Machine is now RUNNING, so it can't be booked again.
        String other = uniqueEmail("late");
        String otherToken = registerCustomer(other);
        setBalance(other, "100.00");
        postAs(otherToken, "/api/bookings", Map.of(
                "machineId", machine.get("id").asLong(),
                "qrToken", machine.get("qrToken").asText()))
            .andExpect(status().isConflict());
    }

    @Test
    void insufficientBalanceIsRejected() throws Exception {
        String attendant = createAttendant();
        JsonNode machine = createMachine(attendant, "30.00");
        String customer = registerCustomer(uniqueEmail("broke"));

        postAs(customer, "/api/bookings", Map.of(
                "machineId", machine.get("id").asLong(),
                "qrToken", machine.get("qrToken").asText()))
            .andExpect(status().isPaymentRequired());
    }

    @Test
    void bookingsArePrivateToOwnerAndAttendants() throws Exception {
        String attendant = createAttendant();
        JsonNode machine = createMachine(attendant, "20.00");
        String email = uniqueEmail("owner");
        String owner = registerCustomer(email);
        setBalance(email, "100.00");
        long bookingId = read(postAs(owner, "/api/bookings", Map.of(
                "machineId", machine.get("id").asLong(),
                "qrToken", machine.get("qrToken").asText()))
            .andExpect(status().isCreated())).get("id").asLong();

        String stranger = registerCustomer(uniqueEmail("stranger"));
        getAs(stranger, "/api/bookings/" + bookingId)
            .andExpect(status().isNotFound());
        postAs(stranger, "/api/bookings/" + bookingId + "/cancel", null)
            .andExpect(status().isNotFound());
        getAs(stranger, "/api/bookings/me")
            .andExpect(jsonPath("$.length()").value(0));

        getAs(owner, "/api/bookings/" + bookingId)
            .andExpect(status().isOk());
        getAs(attendant, "/api/bookings/" + bookingId)
            .andExpect(status().isOk());
    }

    @Test
    void cancellingRefundsAndFreesTheMachine() throws Exception {
        String attendant = createAttendant();
        JsonNode machine = createMachine(attendant, "40.00");
        String email = uniqueEmail("refund");
        String customer = registerCustomer(email);
        setBalance(email, "40.00");

        long bookingId = read(postAs(customer, "/api/bookings", Map.of(
                "machineId", machine.get("id").asLong(),
                "qrToken", machine.get("qrToken").asText()))
            .andExpect(status().isCreated())).get("id").asLong();
        assertThat(balanceOf(email)).isEqualByComparingTo(BigDecimal.ZERO);

        // Attendant-only transitions; illegal jumps are refused.
        mvc.perform(patch("/api/bookings/" + bookingId + "/status")
                .header("Authorization", bearer(customer))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
            .andExpect(status().isForbidden());
        mvc.perform(patch("/api/bookings/" + bookingId + "/status")
                .header("Authorization", bearer(attendant))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
            .andExpect(status().isConflict());

        postAs(customer, "/api/bookings/" + bookingId + "/cancel", null)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(balanceOf(email)).isEqualByComparingTo("40.00");
        getAs(customer, "/api/machines/" + machine.get("id").asLong())
            .andExpect(jsonPath("$.status").value("AVAILABLE"));

        // A cancelled booking can't be cancelled (refunded) twice.
        postAs(customer, "/api/bookings/" + bookingId + "/cancel", null)
            .andExpect(status().isConflict());
        assertThat(balanceOf(email)).isEqualByComparingTo("40.00");
    }

    @Test
    void machinesWithHistoryCannotBeDeleted() throws Exception {
        String attendant = createAttendant();
        JsonNode machine = createMachine(attendant, "25.00");
        String email = uniqueEmail("history");
        String customer = registerCustomer(email);
        setBalance(email, "100.00");
        postAs(customer, "/api/bookings", Map.of(
                "machineId", machine.get("id").asLong(),
                "qrToken", machine.get("qrToken").asText()))
            .andExpect(status().isCreated());

        mvc.perform(org.springframework.test.web.servlet.request
                .MockMvcRequestBuilders.delete(
                    "/api/machines/" + machine.get("id").asLong())
                .header("Authorization", bearer(attendant)))
            .andExpect(status().isConflict());
    }
}
