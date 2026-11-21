package pt.ua.tqs.barkito.fleet.application;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatType;

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
	public Boat register(String code, BoatType type, UUID homeDockId) {
		String normalisedCode = code.trim().toUpperCase(Locale.ROOT);
		if (boats.existsByCode(normalisedCode)) {
			throw new DuplicateBoatCodeException(normalisedCode);
		}
		if (!docks.existsById(homeDockId)) {
			throw new DockNotFoundException(homeDockId);
		}
		return boats.save(Boat.register(normalisedCode, type, homeDockId));
	}

	public Optional<Boat> findById(UUID id) {
		return boats.findById(id);
	}

	/** The operator's fleet list. */
	public List<Boat> findAll() {
		return boats.findAll();
	}
}
