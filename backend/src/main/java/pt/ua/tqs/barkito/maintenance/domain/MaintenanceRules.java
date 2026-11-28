package pt.ua.tqs.barkito.maintenance.domain;

import java.util.Map;
import java.util.Objects;

/**
 * The rule parameters maintenance works with: the maximum of open tickets per technician, set by
 * the operator (BR17); the operating hours (BR6); and the SLA of each ticket type (BR18), set by
 * the client. Until the client backoffice can change them (US-26, US-28), the defaults apply.
 */
public record MaintenanceRules(
    int maxOpenTicketsPerTechnician,
    OperatingHours operatingHours,
    Map<TicketType, SlaPolicy> slaPolicies) {

  /**
   * BR17: the specification leaves the maximum of open tickets to the operator; 5 is the team's
   * default.
   */
  public static final int DEFAULT_MAX_OPEN_TICKETS_PER_TECHNICIAN = 5;

  public static final MaintenanceRules DEFAULT =
      new MaintenanceRules(
          DEFAULT_MAX_OPEN_TICKETS_PER_TECHNICIAN, OperatingHours.DEFAULT, SlaPolicy.DEFAULTS);

  public MaintenanceRules {
    Objects.requireNonNull(operatingHours);
    slaPolicies = Map.copyOf(slaPolicies);
    if (maxOpenTicketsPerTechnician < 1) {
      throw new IllegalArgumentException(
          "A technician must be able to hold at least one open ticket");
    }
    for (TicketType type : TicketType.values()) {
      if (!slaPolicies.containsKey(type)) {
        throw new IllegalArgumentException("No SLA policy for " + type + " tickets");
      }
    }
  }

  public SlaPolicy slaPolicyFor(TicketType type) {
    return slaPolicies.get(type);
  }
}
