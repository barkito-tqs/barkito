package pt.ua.tqs.barkito.fleet.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import pt.ua.tqs.barkito.fleet.domain.Berth;

class InMemoryBerthRepositoryTest {

	private final InMemoryBerthRepository repository = new InMemoryBerthRepository();

	@Test
	void findsSavedBerthsByIdAndListsTheBerthsOfADockByNumber() {
		UUID dock = UUID.randomUUID();
		Berth second = repository.save(Berth.create(dock, 2));
		Berth first = repository.save(Berth.create(dock, 1));
		repository.save(Berth.create(UUID.randomUUID(), 1));

		assertThat(repository.findById(second.id())).contains(second);
		assertThat(repository.findByDockId(dock)).containsExactly(first, second);
	}
}
