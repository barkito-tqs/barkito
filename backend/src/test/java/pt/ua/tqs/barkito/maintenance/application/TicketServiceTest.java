package pt.ua.tqs.barkito.maintenance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import pt.ua.tqs.barkito.fleet.application.DockDetails;
import pt.ua.tqs.barkito.fleet.application.DockService;
import pt.ua.tqs.barkito.fleet.domain.Berth;
import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatState;
import pt.ua.tqs.barkito.fleet.domain.BoatStateChanged;
import pt.ua.tqs.barkito.fleet.domain.BoatStatus;
import pt.ua.tqs.barkito.fleet.domain.BoatType;
import pt.ua.tqs.barkito.fleet.domain.Dock;
import pt.ua.tqs.barkito.fleet.domain.Position;
import pt.ua.tqs.barkito.maintenance.domain.FieldTechnician;
import pt.ua.tqs.barkito.maintenance.domain.FieldTechnicianRepository;
import pt.ua.tqs.barkito.maintenance.domain.Ticket;
import pt.ua.tqs.barkito.maintenance.domain.TicketRepository;
import pt.ua.tqs.barkito.maintenance.domain.TicketStatus;
import pt.ua.tqs.barkito.maintenance.domain.TicketType;

/** US-21: a ticket is opened, assigned (BR17) and given a deadline (BR18) when a boat needs one. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TicketServiceTest {

  private static final ZoneId ZONE = ZoneId.of("Europe/Lisbon");
  private static final Instant TEN_AM = Instant.parse("2026-11-26T10:00:00Z");

  private static final Dock OLD_MARKET =
      new Dock(UUID.randomUUID(), "Old Market", null, new Position(40.641817, -8.66465));
  private static final Dock UNIVERSITY =
      new Dock(UUID.randomUUID(), "University", null, new Position(40.643923, -8.6412));
  private static final Berth MARKET_BERTH = Berth.create(OLD_MARKET.id(), 1);

  private static final FieldTechnician TIAGO =
      new FieldTechnician(UUID.randomUUID(), "Tiago Ferreira", Set.of(OLD_MARKET.id()));
  private static final FieldTechnician RUI =
      new FieldTechnician(UUID.randomUUID(), "Rui Matos", Set.of(UNIVERSITY.id()));

  @Mock private TicketRepository tickets;

  @Mock private FieldTechnicianRepository technicians;

  @Mock private DockService docks;

  private TicketService service;

  @BeforeEach
  void setUp() {
    service = new TicketService(tickets, technicians, docks, Clock.fixed(TEN_AM, ZONE));
    when(docks.findAll())
        .thenReturn(
            List.of(
                new DockDetails(OLD_MARKET, List.of(MARKET_BERTH)),
                new DockDetails(UNIVERSITY, List.of())));
    when(technicians.findAll()).thenReturn(List.of(RUI, TIAGO));
    when(tickets.save(any(Ticket.class))).thenAnswer(call -> call.getArgument(0));
  }

  private static Boat boat(BoatState state, BoatStatus status) {
    return Boat.restore(
        UUID.randomUUID(), "B-07", BoatType.STANDARD, UNIVERSITY.id(), state, status);
  }

  private static BoatStatus mooredAt(Berth berth) {
    return new BoatStatus(64, berth.id(), null, true, true, List.of("MOTOR_OVERHEAT"));
  }

  private Ticket openedTicket() {
    ArgumentCaptor<Ticket> ticket = ArgumentCaptor.forClass(Ticket.class);
    verify(tickets).save(ticket.capture());
    return ticket.getValue();
  }

  @Test
  void aBoatThatNeedsRepairGetsARepairTicketFromTheTechnicianOfItsDock() {
    Boat boat = boat(BoatState.REPAIR_REQUIRED, mooredAt(MARKET_BERTH));

    service.onBoatStateChanged(new BoatStateChanged(boat, BoatState.AVAILABLE));

    Ticket ticket = openedTicket();
    assertThat(ticket.boatId()).isEqualTo(boat.id());
    assertThat(ticket.type()).isEqualTo(TicketType.REPAIR);
    assertThat(ticket.status()).isEqualTo(TicketStatus.OPEN);
    assertThat(ticket.technicianId()).isEqualTo(TIAGO.id());
    assertThat(ticket.openedAt()).isEqualTo(TEN_AM);
    // BR18: REPAIR 24 h within 08:00-22:00 -> 12 h today, 12 h tomorrow from 08:00
    assertThat(ticket.dueAt()).isEqualTo(Instant.parse("2026-11-27T20:00:00Z"));
  }

  @Test
  void aBoatOnTheCanalGoesToTheTechnicianOfTheNearestCoveredDock() {
    Position nearUniversity = new Position(40.6437, -8.6420);
    Boat boat =
        boat(
            BoatState.TOW_REQUIRED,
            new BoatStatus(0, null, nearUniversity, true, false, List.of()));

    service.onBoatStateChanged(new BoatStateChanged(boat, BoatState.IN_USE));

    Ticket ticket = openedTicket();
    assertThat(ticket.type()).isEqualTo(TicketType.TOW);
    assertThat(ticket.technicianId()).isEqualTo(RUI.id());
    assertThat(ticket.onSiteDueAt()).isEqualTo(Instant.parse("2026-11-26T10:30:00Z"));
  }

  @Test
  void aBoatWithoutReadingsIsLocatedAtItsHomeDock() {
    Boat boat = boat(BoatState.CHARGE_REQUIRED, null);

    service.onBoatStateChanged(new BoatStateChanged(boat, BoatState.IN_USE));

    assertThat(openedTicket().technicianId()).isEqualTo(RUI.id());
  }

  @Test
  void aStateThatNeedsNoTechnicianOpensNoTicket() {
    Boat boat = boat(BoatState.AVAILABLE, mooredAt(MARKET_BERTH));

    service.onBoatStateChanged(new BoatStateChanged(boat, BoatState.CHARGING));

    verify(tickets, never()).save(any());
  }

  @Test
  void aBoatWithAnOpenTicketOfTheSameTypeGetsNoSecondOne() {
    Boat boat = boat(BoatState.REPAIR_REQUIRED, mooredAt(MARKET_BERTH));
    when(tickets.existsOpen(boat.id(), TicketType.REPAIR)).thenReturn(true);

    service.onBoatStateChanged(new BoatStateChanged(boat, BoatState.CHARGING));

    verify(tickets, never()).save(any());
  }

  @Test
  void withoutTechniciansTheTicketIsOpenedUnassigned() {
    when(technicians.findAll()).thenReturn(List.of());
    Boat boat = boat(BoatState.REPAIR_REQUIRED, mooredAt(MARKET_BERTH));

    service.onBoatStateChanged(new BoatStateChanged(boat, BoatState.AVAILABLE));

    assertThat(openedTicket().technicianId()).isNull();
  }

  @Test
  void theTicketListNamesEachTechnician() {
    Ticket assigned = ticketFor(TIAGO.id());
    Ticket unassigned = ticketFor(null);
    when(tickets.findAll()).thenReturn(List.of(assigned, unassigned));

    assertThat(service.findAll())
        .extracting(TicketDetails::technicianName)
        .containsExactly("Tiago Ferreira", null);
  }

  private static Ticket ticketFor(UUID technicianId) {
    return new Ticket(
        UUID.randomUUID(),
        UUID.randomUUID(),
        TicketType.CHARGE,
        TicketStatus.OPEN,
        technicianId,
        TEN_AM,
        TEN_AM,
        null);
  }
}
