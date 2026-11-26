package pt.ua.tqs.barkito.maintenance.domain;

/** A ticket stays OPEN until its Field Technician closes it (US-22). */
public enum TicketStatus {
  OPEN,
  CLOSED
}
