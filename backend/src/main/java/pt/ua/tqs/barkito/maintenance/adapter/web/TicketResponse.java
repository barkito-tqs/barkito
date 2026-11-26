package pt.ua.tqs.barkito.maintenance.adapter.web;

import java.time.Instant;
import java.util.UUID;
import pt.ua.tqs.barkito.maintenance.application.TicketDetails;
import pt.ua.tqs.barkito.maintenance.domain.Ticket;
import pt.ua.tqs.barkito.maintenance.domain.TicketStatus;
import pt.ua.tqs.barkito.maintenance.domain.TicketType;

/** A ticket; the technician is null when none could be assigned, the on-site deadline for TOW. */
public record TicketResponse(
    UUID id,
    UUID boatId,
    TicketType type,
    TicketStatus status,
    UUID technicianId,
    String technicianName,
    Instant openedAt,
    Instant dueAt,
    Instant onSiteDueAt) {

  static TicketResponse from(TicketDetails details) {
    Ticket ticket = details.ticket();
    return new TicketResponse(
        ticket.id(),
        ticket.boatId(),
        ticket.type(),
        ticket.status(),
        ticket.technicianId(),
        details.technicianName(),
        ticket.openedAt(),
        ticket.dueAt(),
        ticket.onSiteDueAt());
  }
}
