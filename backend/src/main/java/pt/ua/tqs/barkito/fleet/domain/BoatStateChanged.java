package pt.ua.tqs.barkito.fleet.domain;

import java.util.Objects;

/**
 * Published by the fleet whenever a boat moves to another state, so that other modules can react to
 * it (Product Specification 4.3); maintenance opens a ticket when a boat needs a technician.
 */
public record BoatStateChanged(Boat boat, BoatState previousState) {

  public BoatStateChanged {
    Objects.requireNonNull(boat);
    Objects.requireNonNull(previousState);
  }
}
