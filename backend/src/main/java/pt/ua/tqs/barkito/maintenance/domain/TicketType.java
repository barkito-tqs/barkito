package pt.ua.tqs.barkito.maintenance.domain;

import java.util.Optional;
import pt.ua.tqs.barkito.fleet.domain.BoatState;

/** The work a ticket asks of a Field Technician, one per boat state that waits for one. */
public enum TicketType {
  CHARGE(BoatState.CHARGE_REQUIRED),
  REPAIR(BoatState.REPAIR_REQUIRED),
  TOW(BoatState.TOW_REQUIRED),
  RETURN(BoatState.RETURN_REQUIRED);

  private final BoatState waitingState;

  TicketType(BoatState waitingState) {
    this.waitingState = waitingState;
  }

  /** US-21: the ticket to open when a boat enters this state, if the state waits for one. */
  public static Optional<TicketType> forState(BoatState state) {
    for (TicketType type : values()) {
      if (type.waitingState == state) {
        return Optional.of(type);
      }
    }
    return Optional.empty();
  }
}
