package pt.ua.tqs.barkito.maintenance.adapter.persistence;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import pt.ua.tqs.barkito.maintenance.domain.Ticket;
import pt.ua.tqs.barkito.maintenance.domain.TicketRepository;
import pt.ua.tqs.barkito.maintenance.domain.TicketType;

@Repository
public class InMemoryTicketRepository implements TicketRepository {

  private final Map<UUID, Ticket> tickets = new ConcurrentHashMap<>();

  @Override
  public Ticket save(Ticket ticket) {
    tickets.put(ticket.id(), ticket);
    return ticket;
  }

  @Override
  public List<Ticket> findAll() {
    return tickets.values().stream().sorted(Comparator.comparing(Ticket::dueAt)).toList();
  }

  @Override
  public long countOpenByTechnician(UUID technicianId) {
    return tickets.values().stream()
        .filter(Ticket::isOpen)
        .filter(ticket -> technicianId.equals(ticket.technicianId()))
        .count();
  }

  @Override
  public boolean existsOpen(UUID boatId, TicketType type) {
    return tickets.values().stream()
        .anyMatch(
            ticket -> ticket.isOpen() && ticket.boatId().equals(boatId) && ticket.type() == type);
  }
}
