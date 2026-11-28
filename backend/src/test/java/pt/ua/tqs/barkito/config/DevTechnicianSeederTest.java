package pt.ua.tqs.barkito.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import pt.ua.tqs.barkito.maintenance.adapter.persistence.InMemoryFieldTechnicianRepository;
import pt.ua.tqs.barkito.maintenance.domain.FieldTechnician;

class DevTechnicianSeederTest {

  private final InMemoryFieldTechnicianRepository technicians =
      new InMemoryFieldTechnicianRepository();
  private final DevTechnicianSeeder seeder = new DevTechnicianSeeder(technicians);

  @Test
  void seedsTheTwoTechniciansOfTheWebUi() {
    seeder.run(null);

    assertThat(technicians.findAll())
        .extracting(FieldTechnician::name)
        .containsExactly("Rui Matos", "Tiago Ferreira");
  }

  @Test
  void togetherTheyCoverEverySeededDockOnce() {
    Set<UUID> seededDocks =
        DevDockSeeder.DOCKS.stream()
            .map(seed -> UUID.fromString(seed.id()))
            .collect(Collectors.toSet());

    assertThat(DevTechnicianSeeder.SEEDED_TECHNICIANS)
        .flatExtracting(t -> t.coveredDockIds())
        .containsExactlyInAnyOrderElementsOf(seededDocks);
  }

  @Test
  void runningTwiceSeedsOnce() {
    seeder.run(null);
    seeder.run(null);

    assertThat(technicians.findAll()).hasSize(2);
  }
}
