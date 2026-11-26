package pt.ua.tqs.barkito.bdd;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.assertj.core.api.Assertions;
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
import pt.ua.tqs.barkito.fleet.domain.Position;
import pt.ua.tqs.barkito.maintenance.domain.FieldTechnician;
import pt.ua.tqs.barkito.maintenance.domain.FieldTechnicianRepository;
import pt.ua.tqs.barkito.telemetry.adapter.simulator.SimulatedTelemetrySource;

/**
 * Steps of open_tickets.feature (US-21). Docks, boats and technicians are placed straight in the
 * repositories; the boat state then changes through telemetry, as in the running system.
 */
public class OpenTicketsSteps {

  private static final int BATTERY_PERCENT = 64;

  private final MockMvc mockMvc;
  private final DevTokens devTokens;
  private final DockRepository docks;
  private final BerthRepository berths;
  private final BoatRepository boats;
  private final FieldTechnicianRepository technicians;
  private final SimulatedTelemetrySource telemetry;

  private final Map<String, UUID> dockIds = new HashMap<>();
  private final Map<String, UUID> berthOfBoat = new HashMap<>();

  public OpenTicketsSteps(
      MockMvc mockMvc,
      DevTokens devTokens,
      DockRepository docks,
      BerthRepository berths,
      BoatRepository boats,
      FieldTechnicianRepository technicians,
      SimulatedTelemetrySource telemetry) {
    this.mockMvc = mockMvc;
    this.devTokens = devTokens;
    this.docks = docks;
    this.berths = berths;
    this.boats = boats;
    this.technicians = technicians;
    this.telemetry = telemetry;
  }

  @Given("the dock {string} is at {double}, {double}")
  public void theDockIsAt(String name, double latitude, double longitude) {
    Dock dock =
        docks.save(new Dock(UUID.randomUUID(), name, null, new Position(latitude, longitude)));
    dockIds.put(name, dock.id());
  }

  /** The technician keeps the same id in every scenario, so saving them again moves their docks. */
  @Given("the Field Technician {string} covers the dock {string}")
  public void theTechnicianCoversTheDock(String name, String dock) {
    UUID id = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
    technicians.save(new FieldTechnician(id, name, Set.of(dockIds.get(dock))));
  }

  @Given("the boat {string} is {word} in a berth of the dock {string}")
  public void theBoatIsInABerth(String code, String state, String dock) {
    UUID dockId = dockIds.get(dock);
    int number = berths.findByDockId(dockId).size() + 1;
    Berth berth = berths.save(Berth.create(dockId, number));
    berthOfBoat.put(code, berth.id());
    BoatStatus reading = new BoatStatus(BATTERY_PERCENT, berth.id(), null, true, true, List.of());
    boats.save(
        Boat.restore(
            UUID.randomUUID(), code, BoatType.STANDARD, dockId, BoatState.valueOf(state), reading));
  }

  @When("telemetry reports a fault on the boat {string}")
  public void telemetryReportsAFault(String code) {
    telemetry.publish(reading(code, BATTERY_PERCENT, "\"MOTOR_OVERHEAT\""));
  }

  @When("telemetry reports the boat {string} charged to {int} %")
  public void telemetryReportsTheBoatCharged(String code, int battery) {
    telemetry.publish(reading(code, battery, ""));
  }

  @Then("the operator sees an open {word} ticket for the boat {string} assigned to {string}")
  public void theOperatorSeesAnOpenTicket(String type, String code, String technician)
      throws Exception {
    String ticket = "$[?(@.boatId == '%s')]".formatted(idOf(code));
    tickets()
        .andExpect(jsonPath(ticket + ".type").value(type))
        .andExpect(jsonPath(ticket + ".status").value("OPEN"))
        .andExpect(jsonPath(ticket + ".technicianName").value(technician));
  }

  @Then("the ticket of the boat {string} has an SLA deadline after it was opened")
  public void theTicketHasAnSlaDeadline(String code) throws Exception {
    String body = tickets().andReturn().getResponse().getContentAsString();
    String ticket = "$[?(@.boatId == '%s')]".formatted(idOf(code));
    List<String> openedAt = JsonPath.read(body, ticket + ".openedAt");
    List<String> dueAt = JsonPath.read(body, ticket + ".dueAt");
    Assertions.assertThat(Instant.parse(dueAt.get(0))).isAfter(Instant.parse(openedAt.get(0)));
  }

  @Then("the boat {string} has no ticket")
  public void theBoatHasNoTicket(String code) throws Exception {
    theBoatHasTickets(code, 0);
  }

  @Then("the boat {string} has {int} ticket(s)")
  public void theBoatHasTickets(String code, int count) throws Exception {
    String body = tickets().andReturn().getResponse().getContentAsString();
    List<Object> found = JsonPath.read(body, "$[?(@.boatId == '%s')]".formatted(idOf(code)));
    Assertions.assertThat(found).hasSize(count);
  }

  private ResultActions tickets() throws Exception {
    return mockMvc
        .perform(
            get("/api/v1/tickets")
                .header(HttpHeaders.AUTHORIZATION, devTokens.bearer(Role.OPERATOR)))
        .andExpect(status().isOk());
  }

  private UUID idOf(String code) {
    return boats.findByCode(code).orElseThrow().id();
  }

  private String reading(String code, int battery, String faults) {
    return """
        {"boat":"%s","battery":%d,"berth":"%s","locked":true,"pluggedIn":true,"faults":[%s]}
        """
        .formatted(code, battery, berthOfBoat.get(code), faults);
  }
}
