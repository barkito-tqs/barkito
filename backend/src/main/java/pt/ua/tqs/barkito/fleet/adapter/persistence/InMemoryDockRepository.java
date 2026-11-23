package pt.ua.tqs.barkito.fleet.adapter.persistence;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import pt.ua.tqs.barkito.fleet.domain.Dock;
import pt.ua.tqs.barkito.fleet.domain.DockRepository;

@Repository
public class InMemoryDockRepository implements DockRepository {

  private final Map<UUID, Dock> docks = new ConcurrentHashMap<>();

  @Override
  public Dock save(Dock dock) {
    docks.put(dock.id(), dock);
    return dock;
  }

  @Override
  public Optional<Dock> findById(UUID id) {
    return Optional.ofNullable(docks.get(id));
  }

  @Override
  public List<Dock> findAll() {
    return docks.values().stream().sorted(Comparator.comparing(Dock::name)).toList();
  }

  @Override
  public boolean existsByNameIgnoreCase(String name) {
    return docks.values().stream().anyMatch(dock -> dock.name().equalsIgnoreCase(name));
  }
}
