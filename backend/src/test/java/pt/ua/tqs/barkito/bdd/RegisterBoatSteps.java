package pt.ua.tqs.barkito.bdd;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pt.ua.tqs.barkito.fleet.domain.Dock;
import pt.ua.tqs.barkito.fleet.domain.DockRepository;
import pt.ua.tqs.barkito.fleet.domain.TestDocks;

public class RegisterBoatSteps {

  @Autowired private MockMvc mockMvc;

  @Autowired private DockRepository docks;

  private final Map<String, UUID> dockIds = new HashMap<>();
  private ResultActions response;

  @Given("the dock {string} exists")
  public void theDockExists(String name) {
    Dock dock = docks.save(TestDocks.dock(name));
    dockIds.put(name, dock.id());
  }

  @Given("the boat {string} is registered at the dock {string}")
  public void theBoatIsRegistered(String code, String dock) throws Exception {
    register(code, "STANDARD", dockIds.get(dock)).andExpect(status().isCreated());
  }

  @When("the operator registers the boat {string} of type {string} at the dock {string}")
  public void theOperatorRegistersTheBoat(String code, String type, String dock) throws Exception {
    response = register(code, type, dockIds.get(dock));
  }

  @When("the operator registers the boat {string} of type {string} at a dock that does not exist")
  public void theOperatorRegistersTheBoatAtAnUnknownDock(String code, String type)
      throws Exception {
    response = register(code, type, UUID.randomUUID());
  }

  @Then("the boat is registered with the state {string}")
  public void theBoatIsRegisteredWithTheState(String state) throws Exception {
    response.andExpect(status().isCreated()).andExpect(jsonPath("$.state").value(state));
  }

  @Then("the boat {string} appears in the operator's fleet list")
  public void theBoatAppearsInTheFleetList(String code) throws Exception {
    mockMvc
        .perform(get("/api/v1/boats"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.code == '%s')]".formatted(code)).exists());
  }

  @Then("the registration is refused with the status {int}")
  public void theRegistrationIsRefused(int status) throws Exception {
    response.andExpect(status().is(status));
  }

  private ResultActions register(String code, String type, UUID dockId) throws Exception {
    String body =
        """
				{"code": "%s", "type": "%s", "homeDockId": "%s"}
				"""
            .formatted(code, type, dockId);
    return mockMvc.perform(
        post("/api/v1/boats").contentType(MediaType.APPLICATION_JSON).content(body));
  }
}
