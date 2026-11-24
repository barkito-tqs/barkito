package pt.ua.tqs.barkito.accounts.adapter.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Brings the real filter chain into a {@code @WebMvcTest} slice. Requests authenticate with {@link
 * TestRoles#as}; any bearer token that reaches the decoder is rejected. It stays a
 * {@code @TestConfiguration} so component scanning never picks it up next to the dev decoder.
 */
@TestConfiguration
@Import(SecurityConfig.class)
public class SecuredWebMvcTestConfig {

  @Bean
  JwtDecoder rejectingJwtDecoder() {
    return token -> {
      throw new BadJwtException("No token is valid in a web slice test");
    };
  }
}
