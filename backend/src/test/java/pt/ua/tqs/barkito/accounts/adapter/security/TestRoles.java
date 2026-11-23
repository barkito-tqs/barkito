package pt.ua.tqs.barkito.accounts.adapter.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pt.ua.tqs.barkito.accounts.domain.Role;

/** Authenticates a MockMvc request as a user with one role, skipping the token decoder. */
public final class TestRoles {

  private TestRoles() {}

  public static RequestPostProcessor as(Role role) {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }
}
