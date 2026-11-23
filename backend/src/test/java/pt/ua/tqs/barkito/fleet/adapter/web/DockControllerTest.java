package pt.ua.tqs.barkito.fleet.adapter.web;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pt.ua.tqs.barkito.accounts.adapter.security.TestRoles.as;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pt.ua.tqs.barkito.accounts.adapter.security.SecuredWebMvcTestConfig;
import pt.ua.tqs.barkito.accounts.domain.Role;
import pt.ua.tqs.barkito.fleet.application.DockDetails;
import pt.ua.tqs.barkito.fleet.application.DockService;
import pt.ua.tqs.barkito.fleet.application.DuplicateDockNameException;
import pt.ua.tqs.barkito.fleet.application.InvalidBerthCountException;
import pt.ua.tqs.barkito.fleet.domain.Berth;
import pt.ua.tqs.barkito.fleet.domain.Dock;
import pt.ua.tqs.barkito.fleet.domain.Position;

@WebMvcTest(DockController.class)
@Import(SecuredWebMvcTestConfig.class)
class DockControllerTest {

  private static final Position LOCATION = new Position(40.641817, -8.66465);

  @Autowired private MockMvc mockMvc;

  @MockitoBean private DockService dockService;

  private static DockDetails dockWith(String name, int berthCount) {
    Dock dock = new Dock(UUID.randomUUID(), name, "Mercado", LOCATION);
    List<Berth> berths =
        IntStream.rangeClosed(1, berthCount).mapToObj(n -> Berth.create(dock.id(), n)).toList();
    return new DockDetails(dock, berths);
  }

  private ResultActions register(String json) throws Exception {
    return mockMvc.perform(
        post("/api/v1/docks")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json)
            .with(as(Role.OPERATOR)));
  }

  private static String body(String name, Object latitude, Object longitude, Object berthCount) {
    return """
        {"name": "%s", "district": "Mercado", "latitude": %s, "longitude": %s, "berthCount": %s}
        """
        .formatted(name, latitude, longitude, berthCount);
  }

  @Test
  void registeringADockReturns201WithItsBerths() throws Exception {
    DockDetails details = dockWith("Old Market", 2);
    when(dockService.register("Old Market", "Mercado", LOCATION, 2)).thenReturn(details);

    register(body("Old Market", 40.641817, -8.66465, 2))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/docks/" + details.dock().id()))
        .andExpect(jsonPath("$.name").value("Old Market"))
        .andExpect(jsonPath("$.district").value("Mercado"))
        .andExpect(jsonPath("$.latitude").value(40.641817))
        .andExpect(jsonPath("$.longitude").value(-8.66465))
        .andExpect(jsonPath("$.berthCount").value(2))
        .andExpect(jsonPath("$.berthsInService").value(2))
        .andExpect(jsonPath("$.berths[0].number").value(1))
        .andExpect(jsonPath("$.berths[1].number").value(2))
        .andExpect(jsonPath("$.berths[1].inService").value(true));
  }

  @Test
  void aDuplicateNameIsRefusedWith409() throws Exception {
    when(dockService.register(any(), any(), any(), anyInt()))
        .thenThrow(new DuplicateDockNameException("Old Market"));

    register(body("Old Market", 40.6, -8.6, 2))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.detail").value("A dock named Old Market is already registered."));
  }

  @Test
  void aBerthCountOutsideTheRangeIsRefusedWith422() throws Exception {
    when(dockService.register(any(), any(), any(), anyInt()))
        .thenThrow(InvalidBerthCountException.outOfRange(11));

    register(body("Old Market", 40.6, -8.6, 11))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.detail").value("A dock has from 1 to 10 berths; got 11."));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"latitude\": 40.6, \"longitude\": -8.6, \"berthCount\": 2}",
        "{\"name\": \" \", \"latitude\": 40.6, \"longitude\": -8.6, \"berthCount\": 2}",
        "{\"name\": \"Old Market\", \"longitude\": -8.6, \"berthCount\": 2}",
        "{\"name\": \"Old Market\", \"latitude\": 40.6, \"berthCount\": 2}",
        "{\"name\": \"Old Market\", \"latitude\": 40.6, \"longitude\": -8.6}",
        "{\"name\": \"Old Market\", \"latitude\": 90.0001, \"longitude\": -8.6, \"berthCount\": 2}",
        "{\"name\": \"Old Market\", \"latitude\": -90.0001, \"longitude\": -8.6, \"berthCount\": 2}",
        "{\"name\": \"Old Market\", \"latitude\": 40.6, \"longitude\": 180.0001, \"berthCount\": 2}",
        "{\"name\": \"Old Market\", \"latitude\": 40.6, \"longitude\": -180.0001, \"berthCount\": 2}",
        "{\"name\": \"Old Market\", \"latitude\": 40.6, \"longitude\": -8.6, \"berthCount\": \"x\"}"
      })
  void anIncompleteOrMalformedRequestIsRefusedWith400(String json) throws Exception {
    register(json).andExpect(status().isBadRequest());
    verifyNoInteractions(dockService);
  }

  @Test
  void theLocationBoundariesAreAccepted() throws Exception {
    when(dockService.register(any(), any(), any(), anyInt())).thenReturn(dockWith("Edge", 1));

    register(body("Edge", 90.0, 180.0, 1)).andExpect(status().isCreated());
    register(body("Edge", -90.0, -180.0, 1)).andExpect(status().isCreated());
  }

  @Test
  void aNameOf64CharactersIsAcceptedAnd65IsRefused() throws Exception {
    when(dockService.register(any(), any(), any(), anyInt())).thenReturn(dockWith("Long", 1));

    register(body("a".repeat(64), 40.6, -8.6, 1)).andExpect(status().isCreated());
    register(body("a".repeat(65), 40.6, -8.6, 1)).andExpect(status().isBadRequest());
  }

  @Test
  void aDistrictOf65CharactersIsRefused() throws Exception {
    String json =
        """
        {"name": "Old Market", "district": "%s", "latitude": 40.6, "longitude": -8.6,
         "berthCount": 1}
        """
            .formatted("a".repeat(65));

    register(json).andExpect(status().isBadRequest());
    verifyNoInteractions(dockService);
  }

  @Test
  void listsTheDocks() throws Exception {
    when(dockService.findAll())
        .thenReturn(List.of(dockWith("Arts District", 1), dockWith("Old Market", 3)));

    mockMvc
        .perform(get("/api/v1/docks").with(as(Role.OPERATOR)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].name").value("Arts District"))
        .andExpect(jsonPath("$[1].berthCount").value(3));
  }

  @Test
  void getsADockById() throws Exception {
    DockDetails details = dockWith("Old Market", 3);
    when(dockService.findById(details.dock().id())).thenReturn(Optional.of(details));

    mockMvc
        .perform(get("/api/v1/docks/" + details.dock().id()).with(as(Role.OPERATOR)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Old Market"))
        .andExpect(jsonPath("$.berths", hasSize(3)));
  }

  @Test
  void anUnknownDockIs404() throws Exception {
    UUID unknown = UUID.randomUUID();
    when(dockService.findById(unknown)).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/v1/docks/" + unknown).with(as(Role.OPERATOR)))
        .andExpect(status().isNotFound());
  }
}
