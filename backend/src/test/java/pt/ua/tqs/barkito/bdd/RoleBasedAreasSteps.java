package pt.ua.tqs.barkito.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pt.ua.tqs.barkito.accounts.domain.Role;

/**
 * Steps of role_based_areas.feature (US-02). A request is "let through" when security does not stop
 * it, so the answer of the endpoint itself (such as the 404 of an unknown dock) does not matter.
 */
public class RoleBasedAreasSteps {

  @Autowired private MockMvc mockMvc;

  @Autowired private DevTokens devTokens;

  private Role role;
  private MvcResult response;

  @Given("I am signed in with the role {string}")
  public void iAmSignedInWithTheRole(String roleName) {
    role = Role.valueOf(roleName);
  }

  @Given("I am not signed in")
  public void iAmNotSignedIn() {
    role = null;
  }

  @When("I open the fleet list")
  public void iOpenTheFleetList() throws Exception {
    response = send(get("/api/v1/boats"));
  }

  @When("I look for boats at a dock")
  public void iLookForBoatsAtADock() throws Exception {
    response = send(get("/api/v1/docks/{dockId}/boats", UUID.randomUUID()));
  }

  @Then("the request is let through")
  public void theRequestIsLetThrough() {
    assertThat(response.getResponse().getStatus())
        .isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value());
  }

  @Then("the request is refused with the status {int}")
  public void theRequestIsRefusedWithTheStatus(int expected) {
    assertThat(response.getResponse().getStatus()).isEqualTo(expected);
  }

  private MvcResult send(MockHttpServletRequestBuilder request) throws Exception {
    if (role != null) {
      request.header(HttpHeaders.AUTHORIZATION, devTokens.bearer(role));
    }
    return mockMvc.perform(request).andReturn();
  }
}
