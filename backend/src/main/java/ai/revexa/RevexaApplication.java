package ai.revexa;

import ai.revexa.core.config.RevexaProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Revexa.Ai backend entry point.
 *
 * <p>The application is organised as a set of vertical slices ({@code identity}, {@code catalog},
 * {@code practice}, {@code intelligence}, {@code progress}, {@code sandbox}) that only talk to each
 * other through service interfaces. Each slice can be lifted into its own deployable unit later
 * without touching the packages around it.
 */
@SpringBootApplication
@EnableConfigurationProperties(RevexaProperties.class)
@EnableJpaAuditing
@EnableCaching
@EnableAsync
public class RevexaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RevexaApplication.class, args);
    }
}
