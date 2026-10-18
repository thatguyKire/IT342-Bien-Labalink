package edu.cit.bien.labalink;

import edu.cit.bien.labalink.config.AppProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StripeTestModeGuardTest {

    private static AppProperties.Stripe stripe(String key) {
        return new AppProperties.Stripe(key, "", "php", "s", "c");
    }

    @Test
    void rejectsLiveKeys() {
        assertThatThrownBy(() -> stripe("sk_live_abc"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("live key");
        assertThatThrownBy(() -> stripe("rk_live_abc"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsTestOrMissingKeys() {
        assertThat(stripe("sk_test_abc").isConfigured()).isTrue();
        assertThat(stripe("").isConfigured()).isFalse();
        assertThat(stripe(null).isConfigured()).isFalse();
    }
}
