package pt.ua.tqs.barkito.fleet.adapter.persistence;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import pt.ua.tqs.barkito.fleet.domain.Berth;
import pt.ua.tqs.barkito.fleet.domain.BerthRepository;

@Repository
public class InMemoryBerthRepository implements BerthRepository {

	private final Map<UUID, Berth> berths = new ConcurrentHashMap<>();

	@Override
	public Berth save(Berth berth) {
		berths.put(berth.id(), berth);
		return berth;
	}

	@Override
	public Optional<Berth> findById(UUID id) {
		return Optional.ofNullable(berths.get(id));
	}

	@Override
	public List<Berth> findByDockId(UUID dockId) {
		return berths.values().stream()
				.filter(berth -> berth.dockId().equals(dockId))
				.sorted(Comparator.comparingInt(Berth::number))
				.toList();
	}
}
