package pt.ua.tqs.barkito.accounts.adapter.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class RealmRolesConverterTest {

  private final RealmRolesConverter converter = new RealmRolesConverter();

  private static Jwt withRealmAccess(Object realmAccess) {
    return Jwt.withTokenValue("t")
        .header("alg", "none")
        .subject("someone")
        .claim("realm_access", realmAccess)
        .build();
  }

  private static Jwt withRoles(String... roles) {
    return withRealmAccess(Map.of("roles", List.of(roles)));
  }

  @Test
  void mapsTheRoleInRealmAccessToItsAuthority() {
    assertThat(converter.convert(withRoles("OPERATOR")))
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_OPERATOR");
  }

  @Test
  void mapsEveryKnownRole() {
    Jwt jwt = withRoles("END_USER", "OPERATOR", "TECHNICIAN", "CLIENT_MANAGER", "ADMIN");

    assertThat(converter.convert(jwt))
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly(
            "ROLE_END_USER",
            "ROLE_OPERATOR",
            "ROLE_TECHNICIAN",
            "ROLE_CLIENT_MANAGER",
            "ROLE_ADMIN");
  }

  @Test
  void ignoresRolesThatAreNotBarkitoRoles() {
    Jwt jwt = withRoles("OPERATOR", "offline_access", "default-roles-barkito");

    assertThat(converter.convert(jwt))
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_OPERATOR");
  }

  @Test
  void roleNamesAreCaseSensitive() {
    assertThat(converter.convert(withRoles("operator"))).isEmpty();
  }

  @Test
  void aTokenWithoutRealmAccessHasNoAuthorities() {
    Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("someone").build();

    assertThat(converter.convert(jwt)).isEmpty();
  }

  @Test
  void ignoresRoleEntriesThatAreNullOrNotText() {
    Jwt jwt = withRealmAccess(Map.of("roles", Arrays.asList("OPERATOR", null, 42)));

    assertThat(converter.convert(jwt))
        .extracting(GrantedAuthority::getAuthority)
        .containsExactly("ROLE_OPERATOR");
  }

  @Test
  void aMalformedRealmAccessHasNoAuthorities() {
    assertThat(converter.convert(withRealmAccess("x"))).isEmpty();
    assertThat(converter.convert(withRealmAccess(Map.of("roles", "OPERATOR")))).isEmpty();
  }
}
