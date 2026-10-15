package edu.cit.bien.labalink.support;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Real PostgreSQL (embedded binaries, no Docker) so Flyway migrations,
 * row locks and constraints are exercised exactly as in production.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestInfrastructure {

    private static EmbeddedPostgres postgres;

    public static synchronized EmbeddedPostgres postgres() {
        if (postgres == null) {
            try {
                postgres = EmbeddedPostgres.builder().start();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    postgres.close();
                } catch (IOException ignored) {
                    // best effort
                }
            }));
        }
        return postgres;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        return postgres().getPostgresDatabase();
    }

    @Bean
    @Primary
    public FakePaymentGateway fakePaymentGateway() {
        return new FakePaymentGateway();
    }
}
