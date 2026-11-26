package pt.ua.tqs.barkito.maintenance.domain;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Operator staff who handle the tickets of the docks they cover (BR17). A technician may cover no
 * dock yet; the operator assigns the docks (US-39).
 */
public record FieldTechnician(UUID id, String name, Set<UUID> coveredDockIds) {

  public FieldTechnician {
    Objects.requireNonNull(id);
    Objects.requireNonNull(name);
    coveredDockIds = Set.copyOf(coveredDockIds);
  }

  public boolean covers(UUID dockId) {
    return coveredDockIds.contains(dockId);
  }
}
