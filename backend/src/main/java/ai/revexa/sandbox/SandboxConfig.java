package ai.revexa.sandbox;

import ai.revexa.core.config.RevexaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SandboxConfig {

    @Bean
    @Primary
    CodeExecutionSandbox activeSandbox(RevexaProperties properties, SimulatedSandbox simulated) {
        return "disabled".equalsIgnoreCase(properties.getSandbox().getProvider())
                ? new DisabledSandbox()
                : simulated;
    }
}
