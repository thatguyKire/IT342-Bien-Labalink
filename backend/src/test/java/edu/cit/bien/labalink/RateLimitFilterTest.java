package edu.cit.bien.labalink;

import edu.cit.bien.labalink.security.RateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    @Test
    void authEndpointsAreThrottledPerClient() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(3, 100);

        int last = 0;
        for (int i = 0; i < 4; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request("/api/auth/login", "10.0.0.1"),
                response, new MockFilterChain());
            last = response.getStatus();
        }
        assertThat(last).isEqualTo(429);

        // A different client is unaffected.
        MockHttpServletResponse other = new MockHttpServletResponse();
        filter.doFilter(request("/api/auth/login", "10.0.0.2"),
            other, new MockFilterChain());
        assertThat(other.getStatus()).isEqualTo(200);
    }

    @Test
    void stripeWebhooksAreNeverThrottled() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(1, 1);
        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request("/api/payments/webhook", "3.3.3.3"),
                response, new MockFilterChain());
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    private static MockHttpServletRequest request(String uri, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr(ip);
        return request;
    }
}
