package pt.ua.tqs.barkito.fleet.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import pt.ua.tqs.barkito.fleet.domain.Boat;

/** Port to store boats; the in-memory adapter is replaced by persistence in I3 without changing the service. */
public interface BoatRepository {

	Boat save(Boat boat);

	boolean existsByCode(String code);

	Optional<Boat> findById(UUID id);

	List<Boat> findAll();
}
