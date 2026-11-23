package pt.ua.tqs.barkito.bdd;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;
import pt.ua.tqs.barkito.accounts.domain.Role;

/**
 * Signs real dev tokens, so the scenarios go through the dev decoder and the realm roles converter
 * like a request from the web UI.
 */
@Component
public class DevTokens {

  private static final String ISSUER = "barkito-dev";
  private static final Duration LIFETIME = Duration.ofHours(1);

  private final JwtEncoder encoder;

  public DevTokens(@Value("${barkito.dev.jwt-secret}") String secret) {
    var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }

  /** The value of an Authorization header for a user with the given role. */
  public String bearer(Role role) {
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(ISSUER)
            .subject("dev-" + role.name().toLowerCase(Locale.ROOT))
            .issuedAt(now)
            .expiresAt(now.plus(LIFETIME))
            .claim("realm_access", Map.of("roles", List.of(role.name())))
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return "Bearer " + encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}
