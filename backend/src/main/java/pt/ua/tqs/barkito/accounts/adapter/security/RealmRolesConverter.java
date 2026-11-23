package pt.ua.tqs.barkito.accounts.adapter.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import pt.ua.tqs.barkito.accounts.domain.Role;

/**
 * Reads the roles from the {@code realm_access.roles} claim, where Keycloak puts them, and maps
 * each exact {@link Role} name to the authority {@code ROLE_<NAME>}. Anything else in the claim, or
 * a claim of another shape, grants nothing.
 */
public final class RealmRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  static final String REALM_ACCESS_CLAIM = "realm_access";
  static final String ROLES_KEY = "roles";
  static final String AUTHORITY_PREFIX = "ROLE_";

  private static final Set<String> ROLE_NAMES =
      Arrays.stream(Role.values()).map(Role::name).collect(Collectors.toUnmodifiableSet());

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    if (!(jwt.getClaims().get(REALM_ACCESS_CLAIM) instanceof Map<?, ?> realmAccess)
        || !(realmAccess.get(ROLES_KEY) instanceof Collection<?> roles)) {
      return List.of();
    }
    return roles.stream()
        .filter(role -> role instanceof String name && ROLE_NAMES.contains(name))
        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(AUTHORITY_PREFIX + role))
        .toList();
  }
}
