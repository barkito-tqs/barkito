package pt.ua.tqs.barkito.fleet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.ua.tqs.barkito.fleet.adapter.persistence.InMemoryBerthRepository;
import pt.ua.tqs.barkito.fleet.adapter.persistence.InMemoryBoatRepository;
import pt.ua.tqs.barkito.fleet.adapter.persistence.InMemoryDockRepository;
import pt.ua.tqs.barkito.fleet.domain.Berth;
import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatState;
import pt.ua.tqs.barkito.fleet.domain.BoatStatus;
import pt.ua.tqs.barkito.fleet.domain.BoatType;
import pt.ua.tqs.barkito.fleet.domain.TestDocks;

class AvailableBoatServiceTest {

  private final InMemoryBoatRepository boats = new InMemoryBoatRepository();
  private final InMemoryBerthRepository berths = new InMemoryBerthRepository();
  private final InMemoryDockRepository docks = new InMemoryDockRepository();
  private final AvailableBoatService service = new AvailableBoatService(boats, berths, docks);

  private UUID oldMarket;
  private UUID fishQuay;
  private int berthNumber;

  @BeforeEach
  void createDocks() {
    oldMarket = docks.save(TestDocks.dock("Old Market")).id();
    fishQuay = docks.save(TestDocks.dock("Fish Quay")).id();
  }

  @Test
  void listsOnlyTheBookableBoatsOfTheDockOrderedByCode() {
    moored("B-03", BoatType.STANDARD, BoatState.AVAILABLE, 80, oldMarket);
    moored("B-01", BoatType.XL, BoatState.AVAILABLE, 30, oldMarket);
    moored("B-02", BoatType.STANDARD, BoatState.AVAILABLE, 29, oldMarket);
    moored("B-04", BoatType.STANDARD, BoatState.CHARGING, 80, oldMarket);
    moored("B-05", BoatType.STANDARD, BoatState.AVAILABLE, 80, fishQuay);

    assertThat(service.findAvailableAt(oldMarket))
        .extracting(Boat::code)
        .containsExactly("B-01", "B-03");
  }

  @Test
  void filtersByType() {
    moored("B-01", BoatType.STANDARD, BoatState.AVAILABLE, 80, oldMarket);
    moored("B-02", BoatType.XL, BoatState.AVAILABLE, 80, oldMarket);

    assertThat(service.findAvailableAt(oldMarket, BoatType.XL))
        .extracting(Boat::code)
        .containsExactly("B-02");
  }

  @Test
  void ignoresBoatsOnTheCanal() {
    BoatStatus onTheCanal = new BoatStatus(80, null, null, false, false, List.of());
    boats.save(
        Boat.restore(
            UUID.randomUUID(), "B-01", BoatType.XL, oldMarket, BoatState.AVAILABLE, onTheCanal));

    assertThat(service.findAvailableAt(oldMarket)).isEmpty();
  }

  @Test
  void aDockWithNoAvailableBoatsGivesAnEmptyList() {
    moored("B-01", BoatType.STANDARD, BoatState.CHARGING, 20, fishQuay);

    assertThat(service.findAvailableAt(fishQuay)).isEmpty();
  }

  @Test
  void anUnknownDockIsRefused() {
    UUID unknown = UUID.randomUUID();

    assertThatThrownBy(() -> service.findAvailableAt(unknown))
        .isInstanceOf(DockNotFoundException.class);
  }

  @Test
  void theResultIsAnImmutableList() {
    List<Boat> found = service.findAvailableAt(oldMarket);

    assertThatThrownBy(found::clear).isInstanceOf(UnsupportedOperationException.class);
  }

  private void moored(String code, BoatType type, BoatState state, int battery, UUID dock) {
    berthNumber++;
    Berth berth = berths.save(Berth.create(dock, berthNumber));
    BoatStatus reading = new BoatStatus(battery, berth.id(), null, true, false, List.of());
    boats.save(Boat.restore(UUID.randomUUID(), code, type, dock, state, reading));
  }
}
