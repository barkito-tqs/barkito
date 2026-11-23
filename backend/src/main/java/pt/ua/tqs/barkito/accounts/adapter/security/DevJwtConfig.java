package pt.ua.tqs.barkito.accounts.adapter.security;

import java.nio.charset.StandardCharsets;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Decodes the HS256 tokens of local development, signed with a shared dev secret, until the
 * identity provider (US-01) replaces this decoder.
 */
@Configuration
@Profile("dev")
class DevJwtConfig {

  private static final Logger LOG = LoggerFactory.getLogger(DevJwtConfig.class);

  /** HS256 needs a key of at least 256 bits. */
  static final int MIN_SECRET_BYTES = 32;

  private static final String HMAC_SHA256 = "HmacSHA256";

  @Bean
  JwtDecoder devJwtDecoder(@Value("${barkito.dev.jwt-secret}") String secret) {
    byte[] key = secret.getBytes(StandardCharsets.UTF_8);
    if (key.length < MIN_SECRET_BYTES) {
      throw new IllegalStateException(
          "barkito.dev.jwt-secret must be at least " + MIN_SECRET_BYTES + " bytes");
    }
    LOG.warn(
        "Dev JWT decoder active: tokens signed with the committed dev secret are accepted. "
            + "Never use the dev profile in a deployed environment.");
    return NimbusJwtDecoder.withSecretKey(new SecretKeySpec(key, HMAC_SHA256))
        .macAlgorithm(MacAlgorithm.HS256)
        .build();
  }
}
