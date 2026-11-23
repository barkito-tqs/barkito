package pt.ua.tqs.barkito.fleet.domain;

import java.util.Optional;
import java.util.UUID;

/** Port to read and store docks, shared with dock registration (US-04, BRK-43). */
public interface DockRepository {

  Dock save(Dock dock);

  Optional<Dock> findById(UUID id);

  default boolean existsById(UUID id) {
    return findById(id).isPresent();
  }
}
