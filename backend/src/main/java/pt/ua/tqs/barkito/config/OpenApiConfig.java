package pt.ua.tqs.barkito.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** API documentation for developers (Product Specification, "API for developers"). */
@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI barkitoOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Barkito API")
				.version("v1")
				.description("REST API of Barkito, the shared electric boat service on the canals of Ria Nova."));
	}
}
