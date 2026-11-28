package pt.ua.tqs.barkito.maintenance.domain;

import java.util.List;
import java.util.UUID;

/** Port to store tickets; the in-memory adapter is replaced by persistence in I3 (BRK-413). */
public interface TicketRepository {

  Ticket save(Ticket ticket);

  /** Every ticket, ordered by SLA deadline. */
  List<Ticket> findAll();

  /** The open tickets assigned to a technician. */
  long countOpenByTechnician(UUID technicianId);

  /** The open tickets that have no technician, ordered by SLA deadline. */
  List<Ticket> findOpenUnassigned();

  /** Whether the boat already has an open ticket of this type. */
  boolean existsOpen(UUID boatId, TicketType type);
}
