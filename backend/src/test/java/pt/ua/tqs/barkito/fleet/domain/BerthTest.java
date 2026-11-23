package pt.ua.tqs.barkito.fleet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class BerthTest {

	@Test
	void aNewBerthIsInServiceByDefault() {
		UUID dock = UUID.randomUUID();

		Berth berth = Berth.create(dock, 1);

		assertThat(berth.inService()).isTrue();
		assertThat(berth.dockId()).isEqualTo(dock);
		assertThat(berth.number()).isEqualTo(1);
		assertThat(berth.id()).isNotNull();
	}

	@Test
	void berthNumbersStartAt1() {
		UUID dock = UUID.randomUUID();

		assertThatThrownBy(() -> Berth.create(dock, 0)).isInstanceOf(IllegalArgumentException.class);
	}
}
