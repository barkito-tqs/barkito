package pt.ua.tqs.barkito.fleet.application;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatRepository;
import pt.ua.tqs.barkito.fleet.domain.BoatStateChanged;
import pt.ua.tqs.barkito.fleet.domain.BoatStatus;
import pt.ua.tqs.barkito.fleet.domain.BoatType;
import pt.ua.tqs.barkito.fleet.domain.DockRepository;

/**
 * Fleet use cases for the operator. Every change of a boat's state is published as a {@link
 * BoatStateChanged} event (Product Specification 4.3).
 */
@Service
public class BoatService {

  private final BoatRepository boats;
  private final DockRepository docks;
  private final ApplicationEventPublisher events;

  public BoatService(BoatRepository boats, DockRepository docks, ApplicationEventPublisher events) {
    this.boats = boats;
    this.docks = docks;
    this.events = events;
  }

  /**
   * US-03: registers a boat with a unique code, a type and an existing home dock. The boat starts
   * INACTIVE (lifecycle transition 1).
   */
  public synchronized Boat register(String code, BoatType type, UUID homeDockId) {
    String normalisedCode = code.trim().toUpperCase(Locale.ROOT);
    if (boats.existsByCode(normalisedCode)) {
      throw new DuplicateBoatCodeException(normalisedCode);
    }
    if (!docks.existsById(homeDockId)) {
      throw new DockNotFoundException(homeDockId);
    }
    return boats.save(Boat.register(normalisedCode, type, homeDockId));
  }

  /**
   * Records a telemetry reading for the boat with that code, returning the updated boat, or empty
   * (saving nothing) when no boat has the code. The code is normalised like in {@link #register}.
   *
   * <p>The method is {@code synchronized} so that readings (and {@link #register}) are serialised
   * against each other. It does NOT protect against other writers of {@code Boat}. Future writers
   * (activation, reservation, trip end) must go through this service so they share the lock, or the
   * repository port must offer an atomic per-boat update (or a version check once persistence
   * arrives).
   */
  public synchronized Optional<Boat> recordStatus(String code, BoatStatus status) {
    String normalisedCode = code.trim().toUpperCase(Locale.ROOT);
    return boats.findByCode(normalisedCode).map(boat -> save(boat, boat.applyStatus(status)));
  }

  /**
   * US-05: activates a boat (BR19). Synchronized with {@link #recordStatus} so no reading is lost.
   */
  public synchronized Boat activate(UUID id) {
    Boat boat = find(id);
    return save(boat, boat.activate());
  }

  /**
   * US-05: deactivates a boat (BR19). Synchronized with {@link #recordStatus} so no reading is
   * lost.
   */
  public synchronized Boat deactivate(UUID id) {
    Boat boat = find(id);
    return save(boat, boat.deactivate());
  }

  /**
   * Stores the updated boat and, when its state changed, publishes the change. Listeners run before
   * this returns, still inside the caller's lock.
   */
  private Boat save(Boat before, Boat after) {
    Boat saved = boats.save(after);
    if (saved.state() != before.state()) {
      events.publishEvent(new BoatStateChanged(saved, before.state()));
    }
    return saved;
  }

  private Boat find(UUID id) {
    return boats.findById(id).orElseThrow(() -> new BoatNotFoundException(id));
  }

  public Optional<Boat> findById(UUID id) {
    return boats.findById(id);
  }

  /** The operator's fleet list. */
  public List<Boat> findAll() {
    return boats.findAll();
  }
}
