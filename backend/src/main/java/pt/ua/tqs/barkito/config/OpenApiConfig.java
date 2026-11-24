package pt.ua.tqs.barkito.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** API documentation for developers (Product Specification, "API for developers"). */
@Configuration
public class OpenApiConfig {

  private static final String BEARER_SCHEME = "bearer";

  @Bean
  OpenAPI barkitoOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Barkito API")
                .version("v1")
                .description(
                    "REST API of Barkito, the shared electric boat service on the canals of Ria Nova."))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_SCHEME,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
  }
}
