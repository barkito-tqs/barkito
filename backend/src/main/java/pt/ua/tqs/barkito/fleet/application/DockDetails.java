package pt.ua.tqs.barkito.fleet.application;

import java.util.List;
import pt.ua.tqs.barkito.fleet.domain.Berth;
import pt.ua.tqs.barkito.fleet.domain.Dock;

/** A dock with its berths, ordered by number. */
public record DockDetails(Dock dock, List<Berth> berths) {

  public DockDetails {
    berths = List.copyOf(berths);
  }

  public long berthsInService() {
    return berths.stream().filter(Berth::inService).count();
  }
}
