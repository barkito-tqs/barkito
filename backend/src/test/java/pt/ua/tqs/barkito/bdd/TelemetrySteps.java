package pt.ua.tqs.barkito.bdd;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatRepository;
import pt.ua.tqs.barkito.fleet.domain.BoatState;
import pt.ua.tqs.barkito.fleet.domain.BoatType;
import pt.ua.tqs.barkito.telemetry.adapter.simulator.SimulatedTelemetrySource;

public class TelemetrySteps {

  @Autowired private MockMvc mockMvc;

  @Autowired private BoatRepository boats;

  @Autowired private SimulatedTelemetrySource telemetry;

  @Given("the boat {string} is in the state {string}")
  public void theBoatIsInTheState(String code, String state) {
    boats.save(
        Boat.restore(
            UUID.randomUUID(),
            code,
            BoatType.STANDARD,
            UUID.randomUUID(),
            BoatState.valueOf(state),
            null));
  }

  @When("telemetry sends the reading:")
  public void telemetrySendsTheReading(String reading) {
    telemetry.publish(reading);
  }

  @When("telemetry reports the boat {string} with {int} % battery")
  public void telemetryReportsTheBoat(String code, int battery) {
    telemetry.publish(reading(code, battery, ""));
  }

  @When("telemetry reports the boat {string} with {int} % battery and the fault {string}")
  public void telemetryReportsTheBoatWithAFault(String code, int battery, String fault) {
    telemetry.publish(reading(code, battery, "\"%s\"".formatted(fault)));
  }

  @Then("the boat {string} is now in the state {string}")
  public void theBoatIsNowInTheState(String code, String state) throws Exception {
    mockMvc
        .perform(get("/api/v1/boats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath(boatPath(code, "state")).value(state));
  }

  @Then("the operator sees the boat {string} with {int} % battery, locked and plugged in")
  public void theOperatorSeesTheBoatWithBattery(String code, int battery) throws Exception {
    mockMvc
        .perform(get("/api/v1/boats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath(boatPath(code, "status.batteryPercent")).value(battery))
        .andExpect(jsonPath(boatPath(code, "status.locked")).value(true))
        .andExpect(jsonPath(boatPath(code, "status.pluggedIn")).value(true))
        .andExpect(jsonPath(boatPath(code, "status.faults[0]")).doesNotExist());
  }

  @Then("the operator sees the boat {string} moored in the berth {string}")
  public void theOperatorSeesTheBoatMoored(String code, String berth) throws Exception {
    mockMvc
        .perform(get("/api/v1/boats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath(boatPath(code, "status.berthId")).value(berth));
  }

  @Then("the operator sees the boat {string} without a status")
  public void theOperatorSeesTheBoatWithoutAStatus(String code) throws Exception {
    mockMvc
        .perform(get("/api/v1/boats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.code == '%s')]".formatted(code)).exists())
        .andExpect(jsonPath("$[?(@.code == '%s' && @.status == null)]".formatted(code)).exists());
  }

  @Then("no boat {string} appears in the fleet list")
  public void noBoatAppearsInTheFleetList(String code) throws Exception {
    mockMvc
        .perform(get("/api/v1/boats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.code == '%s')]".formatted(code)).isEmpty());
  }

  private static String boatPath(String code, String field) {
    return "$[?(@.code == '%s')].%s".formatted(code, field);
  }

  private static String reading(String code, int battery, String faults) {
    return """
				{"boat":"%s","battery":%d,"locked":true,"pluggedIn":true,"faults":[%s]}
				"""
        .formatted(code, battery, faults);
  }
}
