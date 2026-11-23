package pt.ua.tqs.barkito.bdd;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pt.ua.tqs.barkito.accounts.domain.Role;
import pt.ua.tqs.barkito.fleet.domain.Berth;
import pt.ua.tqs.barkito.fleet.domain.BerthRepository;
import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatRepository;
import pt.ua.tqs.barkito.fleet.domain.BoatState;
import pt.ua.tqs.barkito.fleet.domain.BoatStatus;
import pt.ua.tqs.barkito.fleet.domain.BoatType;
import pt.ua.tqs.barkito.fleet.domain.Dock;
import pt.ua.tqs.barkito.fleet.domain.DockRepository;
import pt.ua.tqs.barkito.fleet.domain.TestDocks;

/**
 * Steps of find_available_boats.feature (US-09). Boats are placed straight in the repositories,
 * because telemetry (US-07, BRK-45) is what sets the state, battery and berth in the running
 * system.
 */
public class FindAvailableBoatsSteps {

  @Autowired private MockMvc mockMvc;

  @Autowired private DevTokens devTokens;

  @Autowired private DockRepository docks;

  @Autowired private BerthRepository berths;

  @Autowired private BoatRepository boats;

  private final Map<String, UUID> dockIds = new HashMap<>();
  private final Map<String, Integer> berthsUsed = new HashMap<>();
  private ResultActions response;

  @Given("the docks {string} and {string} exist")
  public void theDocksExist(String first, String second) {
    for (String name : new String[] {first, second}) {
      Dock dock = docks.save(TestDocks.dock(name));
      dockIds.put(name, dock.id());
    }
  }

  @Given("the boat {string} of type {string} is {word} at the dock {string} with {int} % battery")
  public void theBoatIsAtTheDock(String code, String type, String state, String dock, int battery) {
    UUID dockId = dockIds.get(dock);
    int number = berthsUsed.merge(dock, 1, Integer::sum);
    Berth berth = berths.save(Berth.create(dockId, number));
    BoatStatus reading = new BoatStatus(battery, berth.id(), null, true, false, List.of());
    boats.save(
        Boat.restore(
            UUID.randomUUID(),
            code,
            BoatType.valueOf(type),
            dockId,
            BoatState.valueOf(state),
            reading));
  }

  @When("the user looks for boats at the dock {string}")
  public void theUserLooksForBoats(String dock) throws Exception {
    response =
        mockMvc.perform(
            get("/api/v1/docks/{dockId}/boats", dockIds.get(dock))
                .header(HttpHeaders.AUTHORIZATION, devTokens.bearer(Role.END_USER)));
  }

  @When("the user looks for boats of type {string} at the dock {string}")
  public void theUserLooksForBoatsOfType(String type, String dock) throws Exception {
    response =
        mockMvc.perform(
            get("/api/v1/docks/{dockId}/boats", dockIds.get(dock))
                .param("type", type)
                .header(HttpHeaders.AUTHORIZATION, devTokens.bearer(Role.END_USER)));
  }

  @Then("the boat {string} is listed")
  public void theBoatIsListed(String code) throws Exception {
    response.andExpect(status().isOk()).andExpect(jsonPath(byCode(code)).exists());
  }

  @Then("the boat {string} is not listed")
  public void theBoatIsNotListed(String code) throws Exception {
    response.andExpect(status().isOk()).andExpect(jsonPath(byCode(code)).doesNotExist());
  }

  @Then("only the boat {string} is listed")
  public void onlyTheBoatIsListed(String code) throws Exception {
    response
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].code").value(code));
  }

  @Then("the boat {string} is shown with the type {string} and {int} % battery")
  public void theBoatIsShownWith(String code, String type, int battery) throws Exception {
    response
        .andExpect(status().isOk())
        .andExpect(jsonPath(byCode(code) + ".type").value(type))
        .andExpect(jsonPath(byCode(code) + ".batteryPercent").value(battery));
  }

  @Then("no boats are listed")
  public void noBoatsAreListed() throws Exception {
    response.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
  }

  private static String byCode(String code) {
    return "$[?(@.code == '%s')]".formatted(code);
  }
}
