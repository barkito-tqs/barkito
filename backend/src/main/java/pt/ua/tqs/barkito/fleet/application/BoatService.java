package pt.ua.tqs.barkito.fleet.application;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatRepository;
import pt.ua.tqs.barkito.fleet.domain.BoatStatus;
import pt.ua.tqs.barkito.fleet.domain.BoatType;
import pt.ua.tqs.barkito.fleet.domain.DockRepository;

/** Fleet use cases for the operator. */
@Service
public class BoatService {

	private final BoatRepository boats;
	private final DockRepository docks;

	public BoatService(BoatRepository boats, DockRepository docks) {
		this.boats = boats;
		this.docks = docks;
	}

	/**
	 * US-03: registers a boat with a unique code, a type and an existing home dock.
	 * The boat starts INACTIVE (lifecycle transition 1).
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
	 * Records a telemetry reading for the boat with that code, returning the updated boat, or
	 * empty (saving nothing) when no boat has the code. The code is normalised like in
	 * {@link #register}.
	 *
	 * <p>The method is {@code synchronized} so that readings (and {@link #register}) are
	 * serialised against each other. It does NOT protect against other writers of {@code Boat}.
	 * Future writers (activation, reservation, trip end) must go through this service so they
	 * share the lock, or the repository port must offer an atomic per-boat update (or a version
	 * check once persistence arrives).
	 */
	public synchronized Optional<Boat> recordStatus(String code, BoatStatus status) {
		String normalisedCode = code.trim().toUpperCase(Locale.ROOT);
		return boats.findByCode(normalisedCode).map(boat -> boats.save(boat.applyStatus(status)));
	}

	public Optional<Boat> findById(UUID id) {
		return boats.findById(id);
	}

	/** The operator's fleet list. */
	public List<Boat> findAll() {
		return boats.findAll();
	}
}
