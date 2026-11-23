package pt.ua.tqs.barkito.accounts.adapter.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import pt.ua.tqs.barkito.accounts.domain.Role;

/** The tokens committed in {@code dev-tokens.env} must work with the dev decoder. */
class DevTokensFileTest {

  /** Maven runs the tests from {@code backend/}. */
  private static final Path TOKENS_FILE = Path.of("..", "dev-tokens.env");

  private static final String TOKEN_PREFIX = "TOKEN_";
  private static final String ALL_ROLES = "ALL";

  private final JwtDecoder decoder = new DevJwtConfig().devJwtDecoder(devSecret());
  private final RealmRolesConverter converter = new RealmRolesConverter();

  private static String devSecret() {
    Properties properties = new Properties();
    try (InputStream in =
        DevTokensFileTest.class
            .getClassLoader()
            .getResourceAsStream("application-dev.properties")) {
      properties.load(in);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read application-dev.properties", e);
    }
    return properties.getProperty("barkito.dev.jwt-secret");
  }

  private static String committedToken(String name) throws IOException {
    String prefix = TOKEN_PREFIX + name + "=";
    return Files.readAllLines(TOKENS_FILE).stream()
        .filter(line -> line.startsWith(prefix))
        .map(line -> line.substring(prefix.length()))
        .findFirst()
        .orElseThrow(() -> new AssertionError(prefix + " is missing from dev-tokens.env"));
  }

  private List<String> authoritiesOf(String token) {
    Jwt jwt = decoder.decode(token);
    return converter.convert(jwt).stream().map(GrantedAuthority::getAuthority).toList();
  }

  @ParameterizedTest
  @EnumSource(Role.class)
  void theCommittedTokenOfEachRoleIsAcceptedWithOnlyThatRole(Role role) throws IOException {
    assertThat(authoritiesOf(committedToken(role.name()))).containsExactly("ROLE_" + role.name());
  }

  @Test
  void theCommittedAllRolesTokenHasEveryRole() throws IOException {
    String[] everyAuthority =
        Arrays.stream(Role.values()).map(role -> "ROLE_" + role.name()).toArray(String[]::new);

    assertThat(authoritiesOf(committedToken(ALL_ROLES))).containsExactlyInAnyOrder(everyAuthority);
  }
}
