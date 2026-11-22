package pt.ua.tqs.barkito.fleet.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port defined by the domain to store boats; the in-memory adapter is replaced by persistence in I3. */
public interface BoatRepository {

	Boat save(Boat boat);

	boolean existsByCode(String code);

	Optional<Boat> findById(UUID id);

	List<Boat> findAll();
}
