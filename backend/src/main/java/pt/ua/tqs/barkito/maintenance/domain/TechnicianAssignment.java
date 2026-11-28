package pt.ua.tqs.barkito.maintenance.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.ToDoubleFunction;
import pt.ua.tqs.barkito.fleet.domain.Position;

/**
 * BR17: picks the Field Technician for a new ticket. The ticket goes to the technician who covers
 * the boat's dock or, when the dock has none or the boat is outside a dock, to the technician of
 * the nearest covered dock. A technician already at the maximum of open tickets is skipped, and the
 * ticket goes to the technician with the fewest open tickets, the nearest one on a tie.
 *
 * @param technicians every Field Technician
 * @param openTickets the number of open tickets of each technician; a missing one has none
 * @param dockPositions the position of every dock
 * @param maxOpenTickets the maximum of open tickets per technician, set by the operator
 */
public record TechnicianAssignment(
    List<FieldTechnician> technicians,
    Map<UUID, Long> openTickets,
    Map<UUID, Position> dockPositions,
    int maxOpenTickets) {

  public TechnicianAssignment {
    technicians = List.copyOf(technicians);
    openTickets = Map.copyOf(openTickets);
    dockPositions = Map.copyOf(dockPositions);
  }

  /**
   * The technician for a ticket on a boat moored at {@code dockId} (null when outside a dock) and
   * located at {@code boatPosition} (null when unknown), or empty when there is no technician.
   */
  public Optional<FieldTechnician> choose(UUID dockId, Position boatPosition) {
    ToDoubleFunction<FieldTechnician> distance = t -> distanceToNearestDock(t, boatPosition);
    Comparator<FieldTechnician> fewestOpenThenNearest =
        Comparator.comparingLong(this::openTicketsOf).thenComparingDouble(distance);

    Optional<FieldTechnician> responsible =
        technicians.stream()
            .filter(t -> dockId != null && t.covers(dockId))
            .min(fewestOpenThenNearest)
            .or(
                () ->
                    technicians.stream()
                        .filter(t -> Double.isFinite(distance.applyAsDouble(t)))
                        .min(Comparator.comparingDouble(distance)));

    return responsible
        .filter(t -> openTicketsOf(t) < maxOpenTickets)
        .or(() -> technicians.stream().min(fewestOpenThenNearest));
  }

  private long openTicketsOf(FieldTechnician technician) {
    return openTickets.getOrDefault(technician.id(), 0L);
  }

  /** The distance to the nearest dock the technician covers, or infinity when it cannot be told. */
  private double distanceToNearestDock(FieldTechnician technician, Position boatPosition) {
    if (boatPosition == null) {
      return Double.POSITIVE_INFINITY;
    }
    return technician.coveredDockIds().stream()
        .map(dockPositions::get)
        .filter(Objects::nonNull)
        .mapToDouble(boatPosition::distanceInMetresTo)
        .min()
        .orElse(Double.POSITIVE_INFINITY);
  }
}
