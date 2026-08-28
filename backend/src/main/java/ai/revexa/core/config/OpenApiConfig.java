package ai.revexa.core.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI revexaOpenApi() {
        final String scheme = "bearerAuth";
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Revexa.Ai API")
                                .version("v1")
                                .description(
                                        "AI mentor for competitive programming: reviews, progressive hints, "
                                                + "complexity analysis, approach comparison and progress tracking.")
                                .contact(new Contact().name("Revexa.Ai"))
                                .license(new License().name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList(scheme))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        scheme,
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")));
    }
}
