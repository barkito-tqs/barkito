package pt.ua.tqs.barkito.maintenance.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pt.ua.tqs.barkito.fleet.domain.Position;

/** BR17: which Field Technician gets a new ticket. */
class TechnicianAssignmentTest {

  private static final int MAX_OPEN = 5;

  private static final UUID OLD_MARKET = UUID.randomUUID();
  private static final UUID STATION = UUID.randomUUID();
  private static final UUID UNIVERSITY = UUID.randomUUID();

  /** Station is about 1 km from Old Market, University about 2 km. */
  private static final Map<UUID, Position> DOCKS =
      Map.of(
          OLD_MARKET, new Position(40.641817, -8.66465),
          STATION, new Position(40.63892, -8.6531),
          UNIVERSITY, new Position(40.643923, -8.6412));

  private static final FieldTechnician TIAGO =
      new FieldTechnician(UUID.randomUUID(), "Tiago Ferreira", Set.of(STATION));
  private static final FieldTechnician RUI =
      new FieldTechnician(UUID.randomUUID(), "Rui Matos", Set.of(UNIVERSITY));

  private static TechnicianAssignment assignment(Map<UUID, Long> openTickets) {
    return new TechnicianAssignment(List.of(TIAGO, RUI), openTickets, DOCKS, MAX_OPEN);
  }

  @Test
  void theTechnicianWhoCoversTheDockGetsTheTicket() {
    assertThat(assignment(Map.of()).choose(UNIVERSITY, DOCKS.get(UNIVERSITY))).contains(RUI);
  }

  @Test
  void aDockWithNoTechnicianGoesToTheTechnicianOfTheNearestCoveredDock() {
    assertThat(assignment(Map.of()).choose(OLD_MARKET, DOCKS.get(OLD_MARKET))).contains(TIAGO);
  }

  @Test
  void aBoatOutsideADockGoesToTheTechnicianOfTheNearestCoveredDock() {
    Position nearUniversity = new Position(40.6437, -8.6420);

    assertThat(assignment(Map.of()).choose(null, nearUniversity)).contains(RUI);
  }

  @ParameterizedTest(name = "with {0} open tickets the ticket goes to {1}")
  @CsvSource({"4, Tiago Ferreira", "5, Rui Matos"})
  void aTechnicianAtTheMaximumOfOpenTicketsIsSkipped(long tiagoOpen, String expected) {
    assertThat(assignment(Map.of(TIAGO.id(), tiagoOpen)).choose(STATION, DOCKS.get(STATION)))
        .map(FieldTechnician::name)
        .contains(expected);
  }

  @Test
  void whenEveryoneIsAtTheMaximumTheFewestOpenTicketsWin() {
    Map<UUID, Long> open = Map.of(TIAGO.id(), 7L, RUI.id(), 6L);

    assertThat(assignment(open).choose(STATION, DOCKS.get(STATION))).contains(RUI);
  }

  @Test
  void onATieOfOpenTicketsTheNearestTechnicianWins() {
    FieldTechnician busy = new FieldTechnician(UUID.randomUUID(), "Ana Costa", Set.of(OLD_MARKET));
    Map<UUID, Long> open = Map.of(busy.id(), 5L, TIAGO.id(), 2L, RUI.id(), 2L);
    TechnicianAssignment assignment =
        new TechnicianAssignment(List.of(busy, TIAGO, RUI), open, DOCKS, MAX_OPEN);

    assertThat(assignment.choose(OLD_MARKET, DOCKS.get(OLD_MARKET))).contains(TIAGO);
  }

  @Test
  void twoTechniciansOfTheSameDockShareItByOpenTickets() {
    FieldTechnician ana = new FieldTechnician(UUID.randomUUID(), "Ana Costa", Set.of(STATION));
    TechnicianAssignment assignment =
        new TechnicianAssignment(
            List.of(TIAGO, ana, RUI), Map.of(TIAGO.id(), 3L, ana.id(), 1L), DOCKS, MAX_OPEN);

    assertThat(assignment.choose(STATION, DOCKS.get(STATION))).contains(ana);
  }

  @Test
  void aBoatWhoseLocationIsUnknownGoesToTheTechnicianWithTheFewestOpenTickets() {
    Map<UUID, Long> open = Map.of(TIAGO.id(), 2L, RUI.id(), 1L);

    assertThat(assignment(open).choose(null, null)).contains(RUI);
  }

  @Test
  void withoutTechniciansNobodyGetsTheTicket() {
    TechnicianAssignment none = new TechnicianAssignment(List.of(), Map.of(), DOCKS, MAX_OPEN);

    assertThat(none.choose(STATION, DOCKS.get(STATION))).isEmpty();
  }
}
