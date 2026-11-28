package pt.ua.tqs.barkito.maintenance.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * BR18: how long a Field Technician has to close a ticket of each type. The clock runs only within
 * the operating hours, except for towing, which also has to reach the boat within 30 minutes. The
 * defaults below apply through {@link MaintenanceRules} until the client sets the agreed SLAs
 * (US-28).
 */
public record SlaPolicy(
    TicketType type, Duration deadline, Duration onSiteDeadline, boolean clockRunsOutsideHours) {

  /**
   * BR18 defaults: TOW on site in 30 min and closed in 2 h; CHARGE 2 h; RETURN 4 h; REPAIR 24 h.
   */
  public static final Map<TicketType, SlaPolicy> DEFAULTS =
      Map.of(
          TicketType.TOW,
          new SlaPolicy(TicketType.TOW, Duration.ofHours(2), Duration.ofMinutes(30), true),
          TicketType.CHARGE,
          new SlaPolicy(TicketType.CHARGE, Duration.ofHours(2), null, false),
          TicketType.RETURN,
          new SlaPolicy(TicketType.RETURN, Duration.ofHours(4), null, false),
          TicketType.REPAIR,
          new SlaPolicy(TicketType.REPAIR, Duration.ofHours(24), null, false));

  public SlaPolicy {
    Objects.requireNonNull(type);
    Objects.requireNonNull(deadline);
  }

  /** BR18: the SLA deadline of a ticket opened at {@code openedAt}. */
  public Instant dueAt(Instant openedAt, OperatingHours hours, ZoneId zone) {
    return clockRunsOutsideHours
        ? openedAt.plus(deadline)
        : hours.addOperatingTime(openedAt, deadline, zone);
  }

  /** BR18: when the technician must be on site, for the ticket types that have such a deadline. */
  public Optional<Instant> onSiteDueAt(Instant openedAt) {
    return Optional.ofNullable(onSiteDeadline).map(openedAt::plus);
  }
}
