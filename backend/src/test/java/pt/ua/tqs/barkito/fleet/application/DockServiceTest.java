package pt.ua.tqs.barkito.fleet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.ua.tqs.barkito.fleet.adapter.persistence.InMemoryBerthRepository;
import pt.ua.tqs.barkito.fleet.adapter.persistence.InMemoryDockRepository;
import pt.ua.tqs.barkito.fleet.domain.Berth;
import pt.ua.tqs.barkito.fleet.domain.Position;

/** US-04: registering docks and berths. */
class DockServiceTest {

  private static final Position LOCATION = new Position(40.641817, -8.66465);

  private final InMemoryDockRepository docks = new InMemoryDockRepository();
  private final InMemoryBerthRepository berths = new InMemoryBerthRepository();
  private final DockService service = new DockService(docks, berths);

  private DockDetails register(String name, int berthCount) {
    return service.register(name, "Mercado", LOCATION, berthCount);
  }

  @Test
  void registersADockWithItsBerthsNumberedFrom1AndInService() {
    DockDetails details = register("Old Market", 4);

    assertThat(details.dock().name()).isEqualTo("Old Market");
    assertThat(details.dock().position()).isEqualTo(LOCATION);
    assertThat(details.berths()).extracting(Berth::number).containsExactly(1, 2, 3, 4);
    assertThat(details.berths()).allMatch(Berth::inService);
    assertThat(docks.findById(details.dock().id())).isPresent();
    assertThat(berths.findByDockId(details.dock().id())).hasSize(4);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 10})
  void acceptsFrom1To10Berths(int berthCount) {
    assertThat(register("Old Market", berthCount).berths()).hasSize(berthCount);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 11, -1})
  void refusesFewerThan1OrMoreThan10Berths(int berthCount) {
    assertThatThrownBy(() -> register("Old Market", berthCount))
        .isInstanceOf(InvalidBerthCountException.class);
    assertThat(docks.findAll()).isEmpty();
  }

  @Test
  void refusesANameAlreadyTakenIgnoringCaseAndSurroundingSpaces() {
    register("Old Market", 4);

    assertThatThrownBy(() -> register("  old MARKET ", 2))
        .isInstanceOf(DuplicateDockNameException.class)
        .hasMessage("A dock named old MARKET is already registered.");
    assertThat(docks.findAll()).hasSize(1);
  }

  @Test
  void addsBerthsThatContinueTheNumberingInService() {
    UUID dock = register("Old Market", 4).dock().id();

    DockDetails details = service.addBerths(dock, 2);

    assertThat(details.berths()).extracting(Berth::number).containsExactly(1, 2, 3, 4, 5, 6);
    assertThat(details.berths()).allMatch(Berth::inService);
  }

  @Test
  void acceptsBerthsUpTo10InTotal() {
    UUID dock = register("Old Market", 9).dock().id();

    assertThat(service.addBerths(dock, 1).berths()).hasSize(10);
  }

  @Test
  void refusesBerthsBeyond10InTotal() {
    UUID dock = register("Old Market", 9).dock().id();

    assertThatThrownBy(() -> service.addBerths(dock, 2))
        .isInstanceOf(InvalidBerthCountException.class)
        .hasMessage("Old Market has 9 berths; adding 2 would exceed the maximum of 10.");
    assertThat(berths.findByDockId(dock)).hasSize(9);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1})
  void refusesAddingFewerThan1Berth(int count) {
    UUID dock = register("Old Market", 4).dock().id();

    assertThatThrownBy(() -> service.addBerths(dock, count))
        .isInstanceOf(InvalidBerthCountException.class);
  }

  @Test
  void refusesAddingBerthsToADockThatDoesNotExist() {
    UUID unknown = UUID.randomUUID();

    assertThatThrownBy(() -> service.addBerths(unknown, 1))
        .isInstanceOf(DockNotFoundException.class);
  }

  @Test
  void findsADockWithItsBerths() {
    UUID dock = register("Old Market", 3).dock().id();

    assertThat(service.findById(dock))
        .hasValueSatisfying(details -> assertThat(details.berths()).hasSize(3));
    assertThat(service.findById(UUID.randomUUID())).isEmpty();
  }

  @Test
  void listsTheDocksByNameWithTheirBerths() {
    register("Waterfront", 2);
    register("Arts District", 1);

    assertThat(service.findAll())
        .extracting(details -> details.dock().name(), details -> details.berths().size())
        .containsExactly(tuple("Arts District", 1), tuple("Waterfront", 2));
  }
}
