package pt.ua.tqs.barkito.fleet.adapter.persistence;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatRepository;

@Repository
public class InMemoryBoatRepository implements BoatRepository {

  private final Map<UUID, Boat> boats = new ConcurrentHashMap<>();

  @Override
  public Boat save(Boat boat) {
    boats.put(boat.id(), boat);
    return boat;
  }

  @Override
  public boolean existsByCode(String code) {
    return boats.values().stream().anyMatch(boat -> boat.code().equals(code));
  }

  @Override
  public Optional<Boat> findById(UUID id) {
    return Optional.ofNullable(boats.get(id));
  }

  @Override
  public List<Boat> findAll() {
    return boats.values().stream().sorted(Comparator.comparing(Boat::code)).toList();
  }
}
