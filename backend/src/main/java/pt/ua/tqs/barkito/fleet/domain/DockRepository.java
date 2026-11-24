package pt.ua.tqs.barkito.fleet.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port to read and store docks (US-04, BRK-43). */
public interface DockRepository {

  Dock save(Dock dock);

  Optional<Dock> findById(UUID id);

  default boolean existsById(UUID id) {
    return findById(id).isPresent();
  }

  /** Every dock, ordered by name. */
  List<Dock> findAll();

  /** Whether a dock already has this name, ignoring case. */
  boolean existsByNameIgnoreCase(String name);
}
