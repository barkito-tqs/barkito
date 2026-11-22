package pt.ua.tqs.barkito.fleet.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class BoatTest {

	private static final int FULL_BATTERY = 100;

	private static Boat boatIn(BoatState state) {
		return Boat.restore(UUID.randomUUID(), "B-16", BoatType.XL, UUID.randomUUID(), state, null);
	}

	private static BoatStatus reading(int batteryPercent, List<String> faults) {
		return new BoatStatus(batteryPercent, null, null, true, true, faults);
	}

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

	@Test
	void aNewlyRegisteredBoatHasNoStatus() {
		assertThat(Boat.register("B-16", BoatType.XL, UUID.randomUUID()).status()).isEmpty();
	}

	@ParameterizedTest
	@CsvSource({"29, CHARGING", "30, AVAILABLE", "100, AVAILABLE"})
	void chargingBoatBecomesAvailableAtThirtyPercent(int batteryPercent, BoatState expected) {
		Boat boat = boatIn(BoatState.CHARGING);
		BoatStatus status = reading(batteryPercent, List.of());

		Boat updated = boat.applyStatus(status);

		assertThat(updated.state()).isEqualTo(expected);
		assertThat(updated.status()).contains(status);
		assertThat(boat.state()).isEqualTo(BoatState.CHARGING);
		assertThat(updated.id()).isEqualTo(boat.id());
	}

	@ParameterizedTest
	@EnumSource(value = BoatState.class, names = {"AVAILABLE", "CHARGING"})
	void faultSendsAvailableOrChargingBoatToRepair(BoatState state) {
		Boat updated = boatIn(state).applyStatus(reading(50, List.of("MOTOR")));

		assertThat(updated.state()).isEqualTo(BoatState.REPAIR_REQUIRED);
	}

	@Test
	void faultTakesPriorityOverFullBattery() {
		Boat updated = boatIn(BoatState.CHARGING).applyStatus(reading(FULL_BATTERY, List.of("MOTOR")));

		assertThat(updated.state()).isEqualTo(BoatState.REPAIR_REQUIRED);
	}

	@ParameterizedTest
	@EnumSource(value = BoatState.class, names = {"INACTIVE", "RESERVED", "IN_USE", "REPAIR_REQUIRED"})
	void otherStatesKeepTheirStateButStoreTheStatus(BoatState state) {
		BoatStatus status = reading(FULL_BATTERY, List.of("MOTOR"));

		Boat updated = boatIn(state).applyStatus(status);

		assertThat(updated.state()).isEqualTo(state);
		assertThat(updated.status()).contains(status);
	}

	@Test
	void aCleanReadingDoesNotLeaveRepair() {
		Boat updated = boatIn(BoatState.REPAIR_REQUIRED).applyStatus(reading(FULL_BATTERY, List.of()));

		assertThat(updated.state()).isEqualTo(BoatState.REPAIR_REQUIRED);
	}
}
