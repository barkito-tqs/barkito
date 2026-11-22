package pt.ua.tqs.barkito.fleet.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatType;

class InMemoryBoatRepositoryTest {

  private final InMemoryBoatRepository repository = new InMemoryBoatRepository();

  @Test
  void findsSavedBoatsByIdAndCodeAndListsThemByCode() {
    UUID dock = UUID.randomUUID();
    Boat b2 = repository.save(Boat.register("B-02", BoatType.STANDARD, dock));
    Boat b1 = repository.save(Boat.register("B-01", BoatType.XL, dock));

    assertThat(repository.findById(b2.id())).contains(b2);
    assertThat(repository.existsByCode("B-01")).isTrue();
    assertThat(repository.existsByCode("B-03")).isFalse();
    assertThat(repository.findAll()).containsExactly(b1, b2);
  }

  @Test
  void findsABoatByCode() {
    Boat b1 = repository.save(Boat.register("B-01", BoatType.XL, UUID.randomUUID()));

    assertThat(repository.findByCode("B-01")).contains(b1);
    assertThat(repository.findByCode("B-09")).isEmpty();
  }
}
