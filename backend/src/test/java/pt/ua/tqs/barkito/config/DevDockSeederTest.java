package pt.ua.tqs.barkito.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import pt.ua.tqs.barkito.fleet.adapter.persistence.InMemoryBerthRepository;
import pt.ua.tqs.barkito.fleet.adapter.persistence.InMemoryDockRepository;
import pt.ua.tqs.barkito.fleet.domain.Berth;
import pt.ua.tqs.barkito.fleet.domain.Dock;

class DevDockSeederTest {

  private static final UUID STATION = UUID.fromString("d0c0a000-0000-4000-8000-000000000004");

  private final InMemoryDockRepository docks = new InMemoryDockRepository();
  private final InMemoryBerthRepository berths = new InMemoryBerthRepository();
  private final DevDockSeeder seeder = new DevDockSeeder(docks, berths);

  @Test
  void seedsTheSixDocksOfTheWebUi() {
    seeder.run(null);

    assertThat(docks.findAll())
        .extracting(Dock::name)
        .containsExactly(
            "Arts District", "Old Market", "Salt Quay", "Station", "University", "Waterfront");
  }

  @Test
  void keepsTheBerthsInServiceOfEachDock() {
    seeder.run(null);

    assertThat(berths.findByDockId(STATION))
        .extracting(Berth::number)
        .containsExactly(1, 2, 3, 4, 5, 6);
    assertThat(berths.findByDockId(STATION)).filteredOn(Berth::inService).hasSize(5);
  }

  @Test
  void everySeededDockRespectsTheBerthLimits() {
    assertThat(DevDockSeeder.DOCKS).allMatch(seed -> Dock.allowsBerthCount(seed.berths()));
  }

  @Test
  void runningTwiceSeedsOnce() {
    seeder.run(null);
    seeder.run(null);

    assertThat(docks.findAll()).hasSize(6);
    assertThat(berths.findByDockId(STATION)).hasSize(6);
  }
}
