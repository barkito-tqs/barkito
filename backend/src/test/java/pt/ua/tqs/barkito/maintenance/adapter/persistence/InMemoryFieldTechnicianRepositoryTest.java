package pt.ua.tqs.barkito.maintenance.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pt.ua.tqs.barkito.maintenance.domain.FieldTechnician;

class InMemoryFieldTechnicianRepositoryTest {

  private final InMemoryFieldTechnicianRepository technicians =
      new InMemoryFieldTechnicianRepository();

  @Test
  void listsTheTechniciansByName() {
    technicians.save(new FieldTechnician(UUID.randomUUID(), "Tiago Ferreira", Set.of()));
    technicians.save(new FieldTechnician(UUID.randomUUID(), "Rui Matos", Set.of()));

    assertThat(technicians.findAll())
        .extracting(FieldTechnician::name)
        .containsExactly("Rui Matos", "Tiago Ferreira");
  }

  @Test
  void savingATechnicianAgainReplacesTheDocksTheyCover() {
    UUID id = UUID.randomUUID();
    UUID dock = UUID.randomUUID();
    technicians.save(new FieldTechnician(id, "Tiago Ferreira", Set.of()));

    technicians.save(new FieldTechnician(id, "Tiago Ferreira", Set.of(dock)));

    assertThat(technicians.findAll()).singleElement().matches(t -> t.covers(dock));
  }
}
