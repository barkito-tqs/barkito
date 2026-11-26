package pt.ua.tqs.barkito.maintenance.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TicketTest {

  private static final ZoneId ZONE = ZoneId.of("Europe/Lisbon");
  private static final Instant TEN_AM = Instant.parse("2026-11-26T10:00:00Z");

  @Test
  void aNewTicketIsOpenWithTheDeadlinesOfItsPolicy() {
    UUID boat = UUID.randomUUID();
    UUID technician = UUID.randomUUID();

    Ticket ticket =
        Ticket.open(
            boat,
            TicketType.TOW,
            technician,
            TEN_AM,
            SlaPolicy.defaultFor(TicketType.TOW),
            OperatingHours.DEFAULT,
            ZONE);

    assertThat(ticket.isOpen()).isTrue();
    assertThat(ticket.boatId()).isEqualTo(boat);
    assertThat(ticket.technicianId()).isEqualTo(technician);
    assertThat(ticket.openedAt()).isEqualTo(TEN_AM);
    assertThat(ticket.dueAt()).isEqualTo(Instant.parse("2026-11-26T12:00:00Z"));
    assertThat(ticket.onSiteDueAt()).isEqualTo(Instant.parse("2026-11-26T10:30:00Z"));
  }

  @Test
  void aTicketWithoutOnSiteDeadlineHasNone() {
    Ticket ticket =
        Ticket.open(
            UUID.randomUUID(),
            TicketType.CHARGE,
            null,
            TEN_AM,
            SlaPolicy.defaultFor(TicketType.CHARGE),
            OperatingHours.DEFAULT,
            ZONE);

    assertThat(ticket.onSiteDueAt()).isNull();
    assertThat(ticket.technicianId()).isNull();
  }
}
