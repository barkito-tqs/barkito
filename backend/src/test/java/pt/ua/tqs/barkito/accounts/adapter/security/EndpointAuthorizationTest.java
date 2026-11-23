package pt.ua.tqs.barkito.accounts.adapter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pt.ua.tqs.barkito.accounts.adapter.security.TestRoles.as;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pt.ua.tqs.barkito.accounts.domain.Role;
import pt.ua.tqs.barkito.fleet.adapter.web.AvailableBoatController;
import pt.ua.tqs.barkito.fleet.adapter.web.BoatController;
import pt.ua.tqs.barkito.fleet.adapter.web.DockController;
import pt.ua.tqs.barkito.fleet.application.AvailableBoatService;
import pt.ua.tqs.barkito.fleet.application.BoatService;
import pt.ua.tqs.barkito.fleet.application.DockService;
import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatType;

/**
 * The authorization matrix (US-02): every endpoint lets in only its role. Add a row to {@link
 * #ENDPOINTS} for every new endpoint.
 */
@WebMvcTest({BoatController.class, AvailableBoatController.class, DockController.class})
@Import(SecuredWebMvcTestConfig.class)
class EndpointAuthorizationTest {

  private static final UUID ID = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

  private static final Endpoint[] ENDPOINTS = {
    new Endpoint(HttpMethod.GET, "/api/v1/boats", Role.OPERATOR),
    new Endpoint(HttpMethod.POST, "/api/v1/boats", Role.OPERATOR),
    new Endpoint(HttpMethod.GET, "/api/v1/boats/" + ID, Role.OPERATOR),
    new Endpoint(HttpMethod.POST, "/api/v1/boats/" + ID + "/activation", Role.OPERATOR),
    new Endpoint(HttpMethod.POST, "/api/v1/boats/" + ID + "/deactivation", Role.OPERATOR),
    new Endpoint(HttpMethod.GET, "/api/v1/docks/" + ID + "/boats", Role.END_USER),
    new Endpoint(HttpMethod.GET, "/api/v1/docks", Role.OPERATOR),
    new Endpoint(HttpMethod.POST, "/api/v1/docks", Role.OPERATOR),
    new Endpoint(HttpMethod.GET, "/api/v1/docks/" + ID, Role.OPERATOR),
  };

  @Autowired private MockMvc mockMvc;

  @MockitoBean private BoatService boatService;

  @MockitoBean private AvailableBoatService availableBoatService;

  @MockitoBean private DockService dockService;

  private record Endpoint(HttpMethod method, String path, Role allowed) {

    MockHttpServletRequestBuilder toRequest() {
      MockHttpServletRequestBuilder builder = request(method, path);
      return method == HttpMethod.POST
          ? builder.contentType(MediaType.APPLICATION_JSON).content("{}")
          : builder;
    }
  }

  static Stream<Endpoint> everyEndpoint() {
    return Arrays.stream(ENDPOINTS);
  }

  static Stream<Arguments> everyEndpointWithEveryRole() {
    return everyEndpoint()
        .flatMap(endpoint -> Arrays.stream(Role.values()).map(r -> Arguments.of(endpoint, r)));
  }

  /** The services answer with a boat, so an allowed request reaches a real response. */
  @BeforeEach
  void servicesAnswer() {
    Boat boat = Boat.register("B-16", BoatType.XL, ID);
    when(boatService.activate(any())).thenReturn(boat);
    when(boatService.deactivate(any())).thenReturn(boat);
  }

  @ParameterizedTest
  @MethodSource("everyEndpointWithEveryRole")
  void eachEndpointLetsInOnlyItsRole(Endpoint endpoint, Role role) throws Exception {
    int status =
        mockMvc.perform(endpoint.toRequest().with(as(role))).andReturn().getResponse().getStatus();

    if (role == endpoint.allowed()) {
      assertThat(status)
          .isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value())
          .isLessThan(HttpStatus.INTERNAL_SERVER_ERROR.value());
    } else {
      assertThat(status).isEqualTo(HttpStatus.FORBIDDEN.value());
    }
  }

  @ParameterizedTest
  @MethodSource("everyEndpoint")
  void aRequestWithoutATokenIsRefusedWith401(Endpoint endpoint) throws Exception {
    mockMvc.perform(endpoint.toRequest()).andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @MethodSource("everyEndpoint")
  void aTokenTheDecoderRejectsIsRefusedWith401(Endpoint endpoint) throws Exception {
    mockMvc
        .perform(endpoint.toRequest().header(HttpHeaders.AUTHORIZATION, "Bearer banana"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void anEndpointWithoutARuleIsRefused() throws Exception {
    mockMvc.perform(get("/api/v1/unmapped").with(as(Role.ADMIN))).andExpect(status().isForbidden());
    mockMvc.perform(get("/api/v1/unmapped")).andExpect(status().isUnauthorized());
  }

  /** A 400 or 404 of an allowed request is forwarded to /error, which must not turn it into 401. */
  @Test
  void theErrorDispatchIsNotRefused() throws Exception {
    int status =
        mockMvc
            .perform(
                get("/error")
                    .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.NOT_FOUND.value())
                    .with(
                        request -> {
                          request.setDispatcherType(DispatcherType.ERROR);
                          return request;
                        }))
            .andReturn()
            .getResponse()
            .getStatus();

    assertThat(status).isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value());
  }
}
