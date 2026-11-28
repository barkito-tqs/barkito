package pt.ua.tqs.barkito.maintenance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.context.ApplicationEventPublisher;
import pt.ua.tqs.barkito.fleet.application.BoatService;
import pt.ua.tqs.barkito.fleet.application.DockDetails;
import pt.ua.tqs.barkito.fleet.application.DockService;
import pt.ua.tqs.barkito.fleet.domain.Berth;
import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatRepository;
import pt.ua.tqs.barkito.fleet.domain.BoatState;
import pt.ua.tqs.barkito.fleet.domain.BoatStateChanged;
import pt.ua.tqs.barkito.fleet.domain.BoatStatus;
import pt.ua.tqs.barkito.fleet.domain.BoatType;
import pt.ua.tqs.barkito.fleet.domain.Dock;
import pt.ua.tqs.barkito.fleet.domain.DockRepository;
import pt.ua.tqs.barkito.fleet.domain.Position;
import pt.ua.tqs.barkito.maintenance.domain.FieldTechnician;
import pt.ua.tqs.barkito.maintenance.domain.FieldTechnicianRepository;
import pt.ua.tqs.barkito.maintenance.domain.MaintenanceRules;
import pt.ua.tqs.barkito.maintenance.domain.OperatingHours;
import pt.ua.tqs.barkito.maintenance.domain.SlaPolicy;
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

  @Mock private BoatService boats;

  private TicketService service;

  @BeforeEach
  void setUp() {
    service = serviceWith(MaintenanceRules.DEFAULT);
    when(docks.findAll())
        .thenReturn(
            List.of(
                new DockDetails(OLD_MARKET, List.of(MARKET_BERTH)),
                new DockDetails(UNIVERSITY, List.of())));
    when(technicians.findAll()).thenReturn(List.of(RUI, TIAGO));
    when(tickets.save(any(Ticket.class))).thenAnswer(call -> call.getArgument(0));
  }

  private TicketService serviceWith(MaintenanceRules rules) {
    return new TicketService(tickets, technicians, docks, boats, rules, Clock.fixed(TEN_AM, ZONE));
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

  @Test
  void theDeadlineComesFromTheConfiguredRules() {
    Map<TicketType, SlaPolicy> policies = new EnumMap<>(SlaPolicy.DEFAULTS);
    policies.put(
        TicketType.REPAIR, new SlaPolicy(TicketType.REPAIR, Duration.ofHours(1), null, false));
    MaintenanceRules rules = new MaintenanceRules(5, OperatingHours.DEFAULT, policies);
    Boat boat = boat(BoatState.REPAIR_REQUIRED, mooredAt(MARKET_BERTH));

    serviceWith(rules).onBoatStateChanged(new BoatStateChanged(boat, BoatState.AVAILABLE));

    assertThat(openedTicket().dueAt()).isEqualTo(Instant.parse("2026-11-26T11:00:00Z"));
  }

  @Test
  void aFailureWhileOpeningATicketIsLoggedAndNotThrown() {
    when(tickets.save(any(Ticket.class))).thenThrow(new IllegalStateException("store is down"));
    Boat boat = boat(BoatState.REPAIR_REQUIRED, mooredAt(MARKET_BERTH));

    assertThatCode(
            () -> service.onBoatStateChanged(new BoatStateChanged(boat, BoatState.AVAILABLE)))
        .doesNotThrowAnyException();
  }

  /** The fleet's state change succeeds even when maintenance fails while handling it. */
  @Test
  void aTelemetryReadingIsStoredEvenWhenTheTicketCannotBeOpened() {
    when(tickets.save(any(Ticket.class))).thenThrow(new IllegalStateException("store is down"));
    BoatRepository boatRepository = mock(BoatRepository.class);
    Boat available = boat(BoatState.AVAILABLE, mooredAt(MARKET_BERTH));
    when(boatRepository.findByCode("B-07")).thenReturn(Optional.of(available));
    when(boatRepository.save(any(Boat.class))).thenAnswer(call -> call.getArgument(0));
    ApplicationEventPublisher events =
        event -> service.onBoatStateChanged((BoatStateChanged) event);
    BoatService fleet = new BoatService(boatRepository, mock(DockRepository.class), events);

    Optional<Boat> updated = fleet.recordStatus("B-07", mooredAt(MARKET_BERTH));

    assertThat(updated).map(Boat::state).contains(BoatState.REPAIR_REQUIRED);
    verify(boatRepository).save(updated.get());
  }

  @Test
  void anUnassignedTicketIsAssignedOnceATechnicianCanTakeIt() {
    Boat boat = boat(BoatState.REPAIR_REQUIRED, mooredAt(MARKET_BERTH));
    Ticket pending = ticketForBoat(boat.id(), null);
    when(tickets.findOpenUnassigned()).thenReturn(List.of(pending));
    when(boats.findById(boat.id())).thenReturn(Optional.of(boat));

    int assigned = service.assignUnassignedTickets();

    assertThat(assigned).isEqualTo(1);
    Ticket ticket = openedTicket();
    assertThat(ticket.id()).isEqualTo(pending.id());
    assertThat(ticket.technicianId()).isEqualTo(TIAGO.id());
    assertThat(ticket.dueAt()).isEqualTo(pending.dueAt());
  }

  @Test
  void aTicketStaysUnassignedWhileNoTechnicianExists() {
    when(technicians.findAll()).thenReturn(List.of());
    Boat boat = boat(BoatState.REPAIR_REQUIRED, mooredAt(MARKET_BERTH));
    when(tickets.findOpenUnassigned()).thenReturn(List.of(ticketForBoat(boat.id(), null)));
    when(boats.findById(boat.id())).thenReturn(Optional.of(boat));

    assertThat(service.assignUnassignedTickets()).isZero();
    verify(tickets, never()).save(any());
  }

  private static Ticket ticketForBoat(UUID boatId, UUID technicianId) {
    return new Ticket(
        UUID.randomUUID(),
        boatId,
        TicketType.REPAIR,
        TicketStatus.OPEN,
        technicianId,
        TEN_AM,
        TEN_AM.plus(Duration.ofHours(24)),
        null);
  }
}
