package pt.ua.tqs.barkito.fleet.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port to read and store the berths of the docks (registered with the dock in US-04, BRK-43). */
public interface BerthRepository {

	Berth save(Berth berth);

	Optional<Berth> findById(UUID id);

	/** The berths of a dock, ordered by number. */
	List<Berth> findByDockId(UUID dockId);
}
