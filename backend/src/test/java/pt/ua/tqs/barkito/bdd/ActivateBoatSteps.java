package pt.ua.tqs.barkito.bdd;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pt.ua.tqs.barkito.accounts.domain.Role;
import pt.ua.tqs.barkito.fleet.domain.BoatRepository;
import pt.ua.tqs.barkito.telemetry.adapter.simulator.SimulatedTelemetrySource;

public class ActivateBoatSteps {

  /** The berth field of a reading; a boat not moored leaves it out. */
  private static final String MOORED = "\"berth\":\"0f8fad5b-d9cb-469f-a165-70867728950e\",";

  private static final int BATTERY_PERCENT = 80;

  private final MockMvc mockMvc;
  private final DevTokens devTokens;
  private final BoatRepository boats;
  private final SimulatedTelemetrySource telemetry;

  private ResultActions response;

  public ActivateBoatSteps(
      MockMvc mockMvc,
      DevTokens devTokens,
      BoatRepository boats,
      SimulatedTelemetrySource telemetry) {
    this.mockMvc = mockMvc;
    this.devTokens = devTokens;
    this.boats = boats;
    this.telemetry = telemetry;
  }

  @Given("telemetry reports the boat {string} moored and plugged in with {int} % battery")
  public void telemetryReportsTheBoatMooredAndPluggedIn(String code, int battery) {
    telemetry.publish(reading(code, battery, MOORED, true));
  }

  @Given("telemetry reports the boat {string} moored but not plugged in")
  public void telemetryReportsTheBoatMooredButNotPluggedIn(String code) {
    telemetry.publish(reading(code, BATTERY_PERCENT, MOORED, false));
  }

  @Given("telemetry reports the boat {string} plugged in but not moored")
  public void telemetryReportsTheBoatPluggedInButNotMoored(String code) {
    telemetry.publish(reading(code, BATTERY_PERCENT, "", true));
  }

  @When("the operator activates the boat {string}")
  public void theOperatorActivatesTheBoat(String code) throws Exception {
    response =
        mockMvc.perform(
            post("/api/v1/boats/{id}/activation", idOf(code))
                .header(HttpHeaders.AUTHORIZATION, devTokens.bearer(Role.OPERATOR)));
  }

  @When("the operator deactivates the boat {string}")
  public void theOperatorDeactivatesTheBoat(String code) throws Exception {
    response =
        mockMvc.perform(
            post("/api/v1/boats/{id}/deactivation", idOf(code))
                .header(HttpHeaders.AUTHORIZATION, devTokens.bearer(Role.OPERATOR)));
  }

  @When("the operator activates a boat that does not exist")
  public void theOperatorActivatesABoatThatDoesNotExist() throws Exception {
    response =
        mockMvc.perform(
            post("/api/v1/boats/{id}/activation", UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, devTokens.bearer(Role.OPERATOR)));
  }

  @Then("the operator's request is refused with the status {int}")
  public void theOperatorsRequestIsRefused(int status) throws Exception {
    response.andExpect(status().is(status));
  }

  private UUID idOf(String code) {
    return boats.findByCode(code).orElseThrow().id();
  }

  private static String reading(String code, int battery, String berth, boolean pluggedIn) {
    return """
				{"boat":"%s","battery":%d,%s"locked":true,"pluggedIn":%b,"faults":[]}
				"""
        .formatted(code, battery, berth, pluggedIn);
  }
}
