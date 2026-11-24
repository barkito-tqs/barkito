package pt.ua.tqs.barkito.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pt.ua.tqs.barkito.accounts.domain.Role;

/**
 * Steps for register_dock.feature and register_berth.feature (US-04, BRK-43).
 *
 * <p>The application context, and so the in-memory docks, is shared by every scenario, and dock
 * names are unique. Each scenario therefore gives its docks a suffix of its own, so "Old Market"
 * here never clashes with the "Old Market" of another scenario or feature.
 */
public class RegisterDockSteps {

  private static final Pattern NUMBER = Pattern.compile("\\d+");
  private static final double LATITUDE = 40.6405;
  private static final double LONGITUDE = -8.6538;

  @Autowired private MockMvc mockMvc;

  @Autowired private DevTokens devTokens;

  private final String suffix = " #" + UUID.randomUUID().toString().substring(0, 8);
  private final Map<String, UUID> dockIds = new HashMap<>();
  private ResultActions response;

  /** The scenario's own name for the dock, keeping any spaces around it. */
  private String scoped(String name) {
    String core = name.strip();
    return name.replace(core, core + suffix);
  }

  private ResultActions register(String name, Object latitude, Object longitude, int berths)
      throws Exception {
    String body =
        """
        {"name": "%s", "latitude": %s, "longitude": %s, "berthCount": %d}
        """
            .formatted(scoped(name), latitude, longitude, berths);
    ResultActions result =
        mockMvc.perform(
            post("/api/v1/docks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .header(HttpHeaders.AUTHORIZATION, devTokens.bearer(Role.OPERATOR)));
    int status = result.andReturn().getResponse().getStatus();
    if (status == 201) {
      String id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
      dockIds.put(name.strip(), UUID.fromString(id));
    }
    return result;
  }

  @Given("the dock {string} is registered with {int} berths")
  public void theDockIsRegistered(String name, int berths) throws Exception {
    register(name, LATITUDE, LONGITUDE, berths).andExpect(status().isCreated());
  }

  @When("the operator registers the dock {string} at {double}, {double} with {int} berths")
  public void theOperatorRegistersTheDockAt(
      String name, double latitude, double longitude, int berths) throws Exception {
    response = register(name, latitude, longitude, berths);
  }

  @When("the operator registers the dock {string} with {int} berths")
  public void theOperatorRegistersTheDock(String name, int berths) throws Exception {
    response = register(name, LATITUDE, LONGITUDE, berths);
  }

  @When("the operator registers the dock {string} without a location")
  public void theOperatorRegistersTheDockWithoutALocation(String name) throws Exception {
    response = register(name, null, null, 1);
  }

  @When("the operator adds {int} berth(s) to the dock {string}")
  public void theOperatorAddsBerths(int count, String name) throws Exception {
    response = addBerths(dockIds.get(name), count);
  }

  @When("the operator adds {int} berth(s) to a dock that does not exist")
  public void theOperatorAddsBerthsToAnUnknownDock(int count) throws Exception {
    response = addBerths(UUID.randomUUID(), count);
  }

  private ResultActions addBerths(UUID dock, int count) throws Exception {
    return mockMvc.perform(
        post("/api/v1/docks/" + dock + "/berths")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"count\": %d}".formatted(count))
            .header(HttpHeaders.AUTHORIZATION, devTokens.bearer(Role.OPERATOR)));
  }

  @Then("^the dock \"([^\"]*)\" has the berths ([\\d, and]+)$")
  public void theDockHasTheBerths(String name, String numbers) throws Exception {
    List<Integer> berths = JsonPath.read(dock(name), "$.berths[*].number");
    assertThat(berths).containsExactlyElementsOf(numbersIn(numbers));
  }

  @Then("^the berths ([\\d, and]+) are in service$")
  public void theBerthsAreInService(String numbers) throws Exception {
    String json = response.andReturn().getResponse().getContentAsString();
    for (int number : numbersIn(numbers)) {
      List<Boolean> inService =
          JsonPath.read(json, "$.berths[?(@.number == %d)].inService".formatted(number));
      assertThat(inService).containsExactly(true);
    }
  }

  @Then("the dock {string} is located at {double}, {double}")
  public void theDockIsLocatedAt(String name, double latitude, double longitude) throws Exception {
    String json = dock(name);
    assertThat(JsonPath.<Double>read(json, "$.latitude")).isEqualTo(latitude);
    assertThat(JsonPath.<Double>read(json, "$.longitude")).isEqualTo(longitude);
  }

  @Then("the dock {string} appears in the list of docks")
  public void theDockAppearsInTheList(String name) throws Exception {
    mockMvc
        .perform(
            get("/api/v1/docks").header(HttpHeaders.AUTHORIZATION, devTokens.bearer(Role.OPERATOR)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.name == '%s')]".formatted(scoped(name))).exists());
  }

  @Then("the dock request is accepted with {int} berths")
  public void theRequestIsAccepted(int berths) throws Exception {
    response.andExpect(status().isCreated()).andExpect(jsonPath("$.berthCount").value(berths));
  }

  @Then("the dock request is refused with the status {int}")
  public void theRequestIsRefused(int status) throws Exception {
    response.andExpect(status().is(status));
  }

  private String dock(String name) throws Exception {
    return mockMvc
        .perform(
            get("/api/v1/docks/" + dockIds.get(name))
                .header(HttpHeaders.AUTHORIZATION, devTokens.bearer(Role.OPERATOR)))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private static List<Integer> numbersIn(String text) {
    Matcher matcher = NUMBER.matcher(text);
    List<Integer> numbers = new ArrayList<>();
    while (matcher.find()) {
      numbers.add(Integer.parseInt(matcher.group()));
    }
    return numbers;
  }
}
