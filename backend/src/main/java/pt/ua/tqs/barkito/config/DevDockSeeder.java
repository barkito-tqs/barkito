package pt.ua.tqs.barkito.config;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.ua.tqs.barkito.fleet.domain.Berth;
import pt.ua.tqs.barkito.fleet.domain.BerthRepository;
import pt.ua.tqs.barkito.fleet.domain.Dock;
import pt.ua.tqs.barkito.fleet.domain.DockRepository;
import pt.ua.tqs.barkito.fleet.domain.Position;

/**
 * Development data: the six docks of Ria Nova that the web UI also uses, with the same fixed ids
 * (frontend/src/api/mockData.ts). Berths out of service are stored as they are, since changing them
 * is the municipality's job (US-27). Runs only with the {@code dev} profile.
 */
@Component
@Profile("dev")
public class DevDockSeeder implements ApplicationRunner {

  /** A dock to seed: its berths are numbered from 1, the first {@code inService} in service. */
  record SeedDock(
      String id,
      String name,
      String district,
      double latitude,
      double longitude,
      int berths,
      int inService) {}

  static final List<SeedDock> DOCKS =
      List.of(
          new SeedDock(
              "d0c0a000-0000-4000-8000-000000000001",
              "Old Market",
              "Mercado",
              40.641817,
              -8.66465,
              6,
              6),
          new SeedDock(
              "d0c0a000-0000-4000-8000-000000000002",
              "Waterfront",
              "Marginal",
              40.636023,
              -8.6426,
              8,
              8),
          new SeedDock(
              "d0c0a000-0000-4000-8000-000000000003",
              "Salt Quay",
              "Salinas",
              40.64524,
              -8.65485,
              4,
              4),
          new SeedDock(
              "d0c0a000-0000-4000-8000-000000000004",
              "Station",
              "Estação",
              40.63892,
              -8.6531,
              6,
              5),
          new SeedDock(
              "d0c0a000-0000-4000-8000-000000000005",
              "University",
              "Campus",
              40.643923,
              -8.6412,
              4,
              4),
          new SeedDock(
              "d0c0a000-0000-4000-8000-000000000006",
              "Arts District",
              "Ribeira",
              40.635233,
              -8.6608,
              4,
              3));

  private final DockRepository docks;
  private final BerthRepository berths;

  public DevDockSeeder(DockRepository docks, BerthRepository berths) {
    this.docks = docks;
    this.berths = berths;
  }

  @Override
  public void run(ApplicationArguments args) {
    for (SeedDock seed : DOCKS) {
      UUID id = UUID.fromString(seed.id());
      if (docks.existsById(id)) {
        continue;
      }
      docks.save(
          new Dock(
              id, seed.name(), seed.district(), new Position(seed.latitude(), seed.longitude())));
      for (int number = 1; number <= seed.berths(); number++) {
        berths.save(new Berth(UUID.randomUUID(), id, number, number <= seed.inService()));
      }
    }
  }
}
