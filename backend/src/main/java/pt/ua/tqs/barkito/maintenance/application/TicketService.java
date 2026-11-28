package pt.ua.tqs.barkito.maintenance.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pt.ua.tqs.barkito.fleet.application.BoatService;
import pt.ua.tqs.barkito.fleet.application.DockDetails;
import pt.ua.tqs.barkito.fleet.application.DockService;
import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatStateChanged;
import pt.ua.tqs.barkito.fleet.domain.BoatStatus;
import pt.ua.tqs.barkito.fleet.domain.Dock;
import pt.ua.tqs.barkito.fleet.domain.Position;
import pt.ua.tqs.barkito.maintenance.domain.FieldTechnician;
import pt.ua.tqs.barkito.maintenance.domain.FieldTechnicianRepository;
import pt.ua.tqs.barkito.maintenance.domain.MaintenanceRules;
import pt.ua.tqs.barkito.maintenance.domain.TechnicianAssignment;
import pt.ua.tqs.barkito.maintenance.domain.Ticket;
import pt.ua.tqs.barkito.maintenance.domain.TicketRepository;
import pt.ua.tqs.barkito.maintenance.domain.TicketType;

/**
 * US-21: opens a ticket whenever a boat enters a state that waits for a Field Technician, assigns
 * it (BR17) and gives it an SLA deadline (BR18).
 */
@Service
public class TicketService {

  private static final Logger LOG = LoggerFactory.getLogger(TicketService.class);

  private final TicketRepository tickets;
  private final FieldTechnicianRepository technicians;
  private final DockService docks;
  private final BoatService boats;
  private final MaintenanceRules rules;
  private final Clock clock;

  public TicketService(
      TicketRepository tickets,
      FieldTechnicianRepository technicians,
      DockService docks,
      BoatService boats,
      MaintenanceRules rules,
      Clock clock) {
    this.tickets = tickets;
    this.technicians = technicians;
    this.docks = docks;
    this.boats = boats;
    this.rules = rules;
    this.clock = clock;
  }

  /**
   * Opens the ticket for the state the boat entered, unless the state needs none or the boat
   * already has an open ticket of that type. {@code synchronized} so that two state changes of the
   * same boat cannot open the same ticket twice.
   *
   * <p>The listener runs inside the fleet's state change, after the boat is stored. A failure here
   * is logged and not thrown, so that a maintenance problem never fails a telemetry reading or an
   * operator action that already happened.
   */
  @EventListener
  public synchronized void onBoatStateChanged(BoatStateChanged event) {
    Boat boat = event.boat();
    try {
      TicketType.forState(boat.state())
          .filter(type -> !tickets.existsOpen(boat.id(), type))
          .ifPresent(type -> open(boat, type));
    } catch (RuntimeException e) {
      LOG.error("Could not open the ticket of boat {} in {}", boat.code(), boat.state(), e);
    }
  }

  private void open(Boat boat, TicketType type) {
    Optional<FieldTechnician> technician = chooseTechnician(boat);
    if (technician.isEmpty()) {
      LOG.warn("No Field Technician to assign the {} ticket of boat {}", type, boat.code());
    }
    tickets.save(
        Ticket.open(
            boat.id(),
            type,
            technician.map(FieldTechnician::id).orElse(null),
            clock.instant(),
            rules.slaPolicyFor(type),
            rules.operatingHours(),
            clock.getZone()));
  }

  /**
   * BR17: assigns the open tickets that were opened while no Field Technician could take them.
   * BRK-62 calls it when the operator assigns technicians to docks. The SLA deadlines do not
   * change.
   *
   * @return how many tickets were assigned
   */
  public synchronized int assignUnassignedTickets() {
    int assigned = 0;
    for (Ticket ticket : tickets.findOpenUnassigned()) {
      Optional<UUID> technician =
          boats.findById(ticket.boatId()).flatMap(this::chooseTechnician).map(FieldTechnician::id);
      if (technician.isPresent()) {
        tickets.save(ticket.assignTo(technician.get()));
        assigned++;
      }
    }
    return assigned;
  }

  private Optional<FieldTechnician> chooseTechnician(Boat boat) {
    List<DockDetails> allDocks = docks.findAll();
    Map<UUID, Position> dockPositions =
        allDocks.stream()
            .map(DockDetails::dock)
            .collect(Collectors.toMap(Dock::id, Dock::position));
    UUID dockId = mooredAt(boat, allDocks).orElse(null);
    Position position = locate(boat, dockId, dockPositions);

    List<FieldTechnician> everyTechnician = technicians.findAll();
    Map<UUID, Long> openTickets =
        everyTechnician.stream()
            .collect(
                Collectors.toMap(FieldTechnician::id, t -> tickets.countOpenByTechnician(t.id())));
    return new TechnicianAssignment(
            everyTechnician, openTickets, dockPositions, rules.maxOpenTicketsPerTechnician())
        .choose(dockId, position);
  }

  /** The dock whose berth the latest reading reports the boat moored in, if any. */
  private static Optional<UUID> mooredAt(Boat boat, List<DockDetails> allDocks) {
    Optional<UUID> berthId = boat.status().map(BoatStatus::berthId);
    return berthId.flatMap(
        berth ->
            allDocks.stream()
                .filter(d -> d.berths().stream().anyMatch(b -> b.id().equals(berth)))
                .map(d -> d.dock().id())
                .findFirst());
  }

  /**
   * Where the boat is: its dock when moored, else the position it last reported, else its home
   * dock, else unknown (null).
   */
  private static Position locate(Boat boat, UUID dockId, Map<UUID, Position> dockPositions) {
    if (dockId != null) {
      return dockPositions.get(dockId);
    }
    return boat.status()
        .map(BoatStatus::position)
        .orElseGet(() -> dockPositions.get(boat.homeDockId()));
  }

  /** The operator's ticket list, ordered by SLA deadline, with the name of each technician. */
  public List<TicketDetails> findAll() {
    Map<UUID, String> names =
        technicians.findAll().stream()
            .collect(Collectors.toMap(FieldTechnician::id, FieldTechnician::name));
    return tickets.findAll().stream()
        .map(ticket -> new TicketDetails(ticket, names.get(ticket.technicianId())))
        .toList();
  }
}
