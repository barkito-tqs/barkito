package pt.ua.tqs.barkito.fleet.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import pt.ua.tqs.barkito.fleet.domain.Berth;
import pt.ua.tqs.barkito.fleet.domain.BerthRepository;
import pt.ua.tqs.barkito.fleet.domain.Dock;
import pt.ua.tqs.barkito.fleet.domain.DockRepository;
import pt.ua.tqs.barkito.fleet.domain.Position;

/**
 * US-04: the operator registers docks and their berths. Registering and adding berths are {@code
 * synchronized} so that two requests cannot take the same name or the same berth numbers.
 */
@Service
public class DockService {

  private final DockRepository docks;
  private final BerthRepository berths;

  public DockService(DockRepository docks, BerthRepository berths) {
    this.docks = docks;
    this.berths = berths;
  }

  /**
   * Registers a dock with a unique name (ignoring case and surrounding spaces) and berths numbered
   * from 1, all in service.
   */
  public synchronized DockDetails register(
      String name, String district, Position position, int berthCount) {
    if (!Dock.allowsBerthCount(berthCount)) {
      throw InvalidBerthCountException.outOfRange(berthCount);
    }
    Dock dock = Dock.register(name, district, position);
    if (docks.existsByNameIgnoreCase(dock.name())) {
      throw new DuplicateDockNameException(dock.name());
    }
    docks.save(dock);
    return new DockDetails(dock, createBerths(dock, 1, berthCount));
  }

  /** Adds berths to a dock, numbered after its last one and in service, up to 10 in total. */
  public synchronized DockDetails addBerths(UUID dockId, int count) {
    Dock dock = docks.findById(dockId).orElseThrow(() -> new DockNotFoundException(dockId));
    if (count < Dock.MIN_BERTHS) {
      throw InvalidBerthCountException.nothingToAdd(count);
    }
    List<Berth> existing = berths.findByDockId(dockId);
    if (!Dock.allowsBerthCount(existing.size() + count)) {
      throw InvalidBerthCountException.aboveMaximum(dock, existing.size(), count);
    }
    int next = existing.stream().mapToInt(Berth::number).max().orElse(0) + 1;
    List<Berth> all = new ArrayList<>(existing);
    all.addAll(createBerths(dock, next, count));
    return new DockDetails(dock, all);
  }

  private List<Berth> createBerths(Dock dock, int firstNumber, int count) {
    List<Berth> created = new ArrayList<>();
    for (int number = firstNumber; number < firstNumber + count; number++) {
      created.add(berths.save(Berth.create(dock.id(), number)));
    }
    return created;
  }

  public Optional<DockDetails> findById(UUID id) {
    return docks.findById(id).map(this::withBerths);
  }

  /** Every dock with its berths, ordered by name. */
  public List<DockDetails> findAll() {
    return docks.findAll().stream().map(this::withBerths).toList();
  }

  private DockDetails withBerths(Dock dock) {
    return new DockDetails(dock, berths.findByDockId(dock.id()));
  }
}
