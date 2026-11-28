package pt.ua.tqs.barkito.maintenance.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

/**
 * Work for a Field Technician on one boat (US-21). The technician is null only when no Field
 * Technician exists to take it; the on-site deadline exists only for towing (BR18).
 */
public record Ticket(
    UUID id,
    UUID boatId,
    TicketType type,
    TicketStatus status,
    UUID technicianId,
    Instant openedAt,
    Instant dueAt,
    Instant onSiteDueAt) {

  public Ticket {
    Objects.requireNonNull(id);
    Objects.requireNonNull(boatId);
    Objects.requireNonNull(type);
    Objects.requireNonNull(status);
    Objects.requireNonNull(openedAt);
    Objects.requireNonNull(dueAt);
  }

  /** A new open ticket with the SLA deadlines of its policy (BR18). */
  public static Ticket open(
      UUID boatId,
      TicketType type,
      UUID technicianId,
      Instant openedAt,
      SlaPolicy policy,
      OperatingHours hours,
      ZoneId zone) {
    return new Ticket(
        UUID.randomUUID(),
        boatId,
        type,
        TicketStatus.OPEN,
        technicianId,
        openedAt,
        policy.dueAt(openedAt, hours, zone),
        policy.onSiteDueAt(openedAt).orElse(null));
  }

  /** The same ticket, assigned to a technician (BR17). */
  public Ticket assignTo(UUID newTechnicianId) {
    return new Ticket(id, boatId, type, status, newTechnicianId, openedAt, dueAt, onSiteDueAt);
  }

  public boolean isOpen() {
    return status == TicketStatus.OPEN;
  }
}
