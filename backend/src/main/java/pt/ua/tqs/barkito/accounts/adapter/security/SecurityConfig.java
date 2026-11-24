package pt.ua.tqs.barkito.accounts.adapter.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import pt.ua.tqs.barkito.accounts.domain.Role;

/**
 * Who may reach each endpoint (US-02). Every endpoint is granted to one role; anything without a
 * rule is refused. Every new endpoint needs a rule here and a row in the authorization matrix test.
 */
@Configuration
class SecurityConfig {

  @Bean
  SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            requests ->
                requests
                    .dispatcherTypeMatchers(DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers(
                        "/actuator/health/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers("/api/v1/boats/**")
                    .hasRole(Role.OPERATOR.name())
                    .requestMatchers(HttpMethod.GET, "/api/v1/docks/*/boats")
                    .hasRole(Role.END_USER.name())
                    // US-04: registering and reading docks; after the end-user rule above
                    .requestMatchers("/api/v1/docks/**")
                    .hasRole(Role.OPERATOR.name())
                    .anyRequest()
                    .denyAll())
        .oauth2ResourceServer(
            resourceServer -> resourceServer.jwt(jwt -> jwt.jwtAuthenticationConverter(roles())))
        .build();
  }

  private static JwtAuthenticationConverter roles() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new RealmRolesConverter());
    return converter;
  }
}
