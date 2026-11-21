package pt.ua.tqs.barkito.fleet.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class BoatTest {

	@Test
	void newlyRegisteredBoatStartsInactive() {
		UUID dock = UUID.randomUUID();

		Boat boat = Boat.register("B-16", BoatType.XL, dock);

		assertThat(boat.state()).isEqualTo(BoatState.INACTIVE);
		assertThat(boat.code()).isEqualTo("B-16");
		assertThat(boat.type()).isEqualTo(BoatType.XL);
		assertThat(boat.homeDockId()).isEqualTo(dock);
		assertThat(boat.id()).isNotNull();
	}
}
