package pt.ua.tqs.barkito.maintenance.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pt.ua.tqs.barkito.maintenance.domain.Ticket;
import pt.ua.tqs.barkito.maintenance.domain.TicketStatus;
import pt.ua.tqs.barkito.maintenance.domain.TicketType;

class InMemoryTicketRepositoryTest {

  private static final UUID BOAT = UUID.randomUUID();
  private static final UUID TIAGO = UUID.randomUUID();

  private final InMemoryTicketRepository tickets = new InMemoryTicketRepository();

  private static Ticket ticket(TicketType type, TicketStatus status, String dueAt) {
    Instant due = Instant.parse(dueAt);
    return new Ticket(UUID.randomUUID(), BOAT, type, status, TIAGO, due, due, null);
  }

  @Test
  void listsTheTicketsBySlaDeadline() {
    Ticket later =
        tickets.save(ticket(TicketType.REPAIR, TicketStatus.OPEN, "2026-11-27T20:00:00Z"));
    Ticket sooner =
        tickets.save(ticket(TicketType.CHARGE, TicketStatus.OPEN, "2026-11-26T12:00:00Z"));

    assertThat(tickets.findAll()).containsExactly(sooner, later);
  }

  @Test
  void countsOnlyTheOpenTicketsOfATechnician() {
    tickets.save(ticket(TicketType.REPAIR, TicketStatus.OPEN, "2026-11-27T20:00:00Z"));
    tickets.save(ticket(TicketType.CHARGE, TicketStatus.CLOSED, "2026-11-26T12:00:00Z"));

    assertThat(tickets.countOpenByTechnician(TIAGO)).isEqualTo(1);
    assertThat(tickets.countOpenByTechnician(UUID.randomUUID())).isZero();
  }

  @Test
  void findsTheOpenTicketsWithoutTechnician() {
    Ticket assigned =
        tickets.save(ticket(TicketType.REPAIR, TicketStatus.OPEN, "2026-11-27T20:00:00Z"));
    Ticket pending = tickets.save(assigned.assignTo(null));
    tickets.save(
        new Ticket(
            UUID.randomUUID(),
            BOAT,
            TicketType.TOW,
            TicketStatus.CLOSED,
            null,
            pending.openedAt(),
            pending.dueAt(),
            null));

    assertThat(tickets.findOpenUnassigned()).containsExactly(pending);
  }

  @Test
  void findsAnOpenTicketOfABoatByType() {
    tickets.save(ticket(TicketType.REPAIR, TicketStatus.OPEN, "2026-11-27T20:00:00Z"));
    tickets.save(ticket(TicketType.CHARGE, TicketStatus.CLOSED, "2026-11-26T12:00:00Z"));

    assertThat(tickets.existsOpen(BOAT, TicketType.REPAIR)).isTrue();
    assertThat(tickets.existsOpen(BOAT, TicketType.CHARGE)).isFalse();
    assertThat(tickets.existsOpen(UUID.randomUUID(), TicketType.REPAIR)).isFalse();
  }
}
