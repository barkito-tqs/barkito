package pt.ua.tqs.barkito.accounts.adapter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class DevJwtConfigTest {

  private static final String SECRET = "a-test-secret-of-at-least-32-bytes-long";
  private static final String OTHER_SECRET = "another-test-secret-of-at-least-32-bytes";

  /** Well past the decoder's default clock skew of one minute. */
  private static final Duration LONG_AGO = Duration.ofHours(1);

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withUserConfiguration(DevJwtConfig.class)
          .withPropertyValues("barkito.dev.jwt-secret=" + SECRET);

  private final ApplicationContextRunner devRunner =
      runner.withPropertyValues("spring.profiles.active=dev");

  private static String token(String secret, Instant expiresAt) {
    var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject("dev-operator")
            .issuedAt(expiresAt.minus(LONG_AGO))
            .expiresAt(expiresAt)
            .claim("realm_access", Map.of("roles", List.of("OPERATOR")))
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  private static String validToken(String secret) {
    return token(secret, Instant.now().plus(LONG_AGO));
  }

  @Test
  void thereIsNoDevDecoderOutsideTheDevProfile() {
    runner.run(context -> assertThat(context).doesNotHaveBean(JwtDecoder.class));
  }

  @Test
  void theDevProfileDecodesATokenSignedWithTheSecret() {
    devRunner.run(
        context -> {
          Map<String, Object> realmAccess =
              context.getBean(JwtDecoder.class).decode(validToken(SECRET)).getClaim("realm_access");

          assertThat(realmAccess).containsEntry("roles", List.of("OPERATOR"));
        });
  }

  @Test
  void aTokenSignedWithAnotherSecretIsRejected() {
    devRunner.run(
        context -> {
          JwtDecoder decoder = context.getBean(JwtDecoder.class);

          assertThatThrownBy(() -> decoder.decode(validToken(OTHER_SECRET)))
              .isInstanceOf(BadJwtException.class);
        });
  }

  @Test
  void anExpiredTokenIsRejected() {
    devRunner.run(
        context -> {
          JwtDecoder decoder = context.getBean(JwtDecoder.class);
          String expired = token(SECRET, Instant.now().minus(LONG_AGO));

          assertThatThrownBy(() -> decoder.decode(expired))
              .isInstanceOf(JwtValidationException.class);
        });
  }

  @Test
  void aSecretShorterThan32BytesStopsTheApplication() {
    devRunner
        .withPropertyValues("barkito.dev.jwt-secret=short")
        .run(
            context ->
                assertThat(context.getStartupFailure())
                    .isNotNull()
                    .hasRootCauseMessage("barkito.dev.jwt-secret must be at least 32 bytes"));
  }
}
