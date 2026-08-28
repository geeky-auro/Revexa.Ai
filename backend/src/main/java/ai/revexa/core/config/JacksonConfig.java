package ai.revexa.core.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    Jackson2ObjectMapperBuilderCustomizer revexaJacksonCustomizer() {
        return builder ->
                builder.featuresToDisable(
                                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                        .featuresToEnable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }
}
