package pt.ua.tqs.barkito.maintenance.application;

import java.util.Objects;
import pt.ua.tqs.barkito.maintenance.domain.Ticket;

/** A ticket with the name of its Field Technician, null when it has none. */
public record TicketDetails(Ticket ticket, String technicianName) {

  public TicketDetails {
    Objects.requireNonNull(ticket);
  }
}
