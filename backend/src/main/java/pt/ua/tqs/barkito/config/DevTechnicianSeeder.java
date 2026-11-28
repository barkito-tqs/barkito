package pt.ua.tqs.barkito.config;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ua.tqs.barkito.maintenance.domain.FieldTechnician;
import pt.ua.tqs.barkito.maintenance.domain.FieldTechnicianRepository;

/**
 * Development data: the two Field Technicians of the web UI, covering the docks seeded by {@link
 * DevDockSeeder} as in frontend/src/api/mockData.ts, until the operator assigns docks (US-39). Runs
 * only with the {@code dev} profile.
 */
@Component
@Profile("dev")
public class DevTechnicianSeeder implements ApplicationRunner {

  static final List<FieldTechnician> SEEDED_TECHNICIANS =
      List.of(
          new FieldTechnician(
              UUID.fromString("7ec40000-0000-4000-8000-000000000001"),
              "Tiago Ferreira",
              Set.of(dock(1), dock(2), dock(6))),
          new FieldTechnician(
              UUID.fromString("7ec40000-0000-4000-8000-000000000002"),
              "Rui Matos",
              Set.of(dock(3), dock(4), dock(5))));

  private final FieldTechnicianRepository technicians;

  public DevTechnicianSeeder(FieldTechnicianRepository technicians) {
    this.technicians = technicians;
  }

  /** The id of the n-th dock of {@link DevDockSeeder#DOCKS}. */
  private static UUID dock(int number) {
    return UUID.fromString(DevDockSeeder.DOCKS.get(number - 1).id());
  }

  @Override
  public void run(ApplicationArguments args) {
    SEEDED_TECHNICIANS.forEach(technicians::save);
  }
}
