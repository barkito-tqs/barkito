package pt.ua.tqs.barkito.fleet.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import pt.ua.tqs.barkito.fleet.domain.Dock;
import pt.ua.tqs.barkito.fleet.domain.TestDocks;

class InMemoryDockRepositoryTest {

  private final InMemoryDockRepository docks = new InMemoryDockRepository();

  @Test
  void findsASavedDockById() {
    Dock dock = docks.save(TestDocks.dock("Old Market"));

    assertThat(docks.findById(dock.id())).contains(dock);
    assertThat(docks.existsById(dock.id())).isTrue();
  }

  @Test
  void listsTheDocksOrderedByName() {
    docks.save(TestDocks.dock("Waterfront"));
    docks.save(TestDocks.dock("Arts District"));
    docks.save(TestDocks.dock("Old Market"));

    assertThat(docks.findAll())
        .extracting(Dock::name)
        .containsExactly("Arts District", "Old Market", "Waterfront");
  }

  @Test
  void findsANameIgnoringCase() {
    docks.save(TestDocks.dock("Old Market"));

    assertThat(docks.existsByNameIgnoreCase("old MARKET")).isTrue();
    assertThat(docks.existsByNameIgnoreCase("Old Marke")).isFalse();
  }
}
