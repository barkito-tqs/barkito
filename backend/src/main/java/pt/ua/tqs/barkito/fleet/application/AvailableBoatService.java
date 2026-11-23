package pt.ua.tqs.barkito.fleet.application;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import pt.ua.tqs.barkito.fleet.domain.Berth;
import pt.ua.tqs.barkito.fleet.domain.BerthRepository;
import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatRepository;
import pt.ua.tqs.barkito.fleet.domain.BoatStatus;
import pt.ua.tqs.barkito.fleet.domain.BoatType;
import pt.ua.tqs.barkito.fleet.domain.DockRepository;

/** US-09: the boats an end user can book at a dock. */
@Service
public class AvailableBoatService {

  private final BoatRepository boats;
  private final BerthRepository berths;
  private final DockRepository docks;

  public AvailableBoatService(BoatRepository boats, BerthRepository berths, DockRepository docks) {
    this.boats = boats;
    this.berths = berths;
    this.docks = docks;
  }

  /**
   * The bookable boats moored at the dock (berth from the latest telemetry reading), of any type,
   * ordered by code.
   */
  public List<Boat> findAvailableAt(UUID dockId) {
    return findAvailableAt(dockId, boat -> true);
  }

  /** The bookable boats of one type moored at the dock, ordered by code. */
  public List<Boat> findAvailableAt(UUID dockId, BoatType type) {
    return findAvailableAt(dockId, boat -> boat.type() == type);
  }

  private List<Boat> findAvailableAt(UUID dockId, Predicate<Boat> filter) {
    if (!docks.existsById(dockId)) {
      throw new DockNotFoundException(dockId);
    }
    Set<UUID> berthsOfDock =
        berths.findByDockId(dockId).stream().map(Berth::id).collect(Collectors.toSet());
    return boats.findAll().stream()
        .filter(
            boat ->
                boat.status().map(BoatStatus::berthId).filter(berthsOfDock::contains).isPresent())
        .filter(Boat::isBookable)
        .filter(filter)
        .sorted(Comparator.comparing(Boat::code))
        .toList();
  }
}
