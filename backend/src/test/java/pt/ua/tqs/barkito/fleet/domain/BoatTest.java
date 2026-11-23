package pt.ua.tqs.barkito.fleet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

	private static final UUID BERTH = UUID.randomUUID();

	private static BoatStatus reading(int batteryPercent, List<String> faults) {
		return new BoatStatus(batteryPercent, null, null, true, true, faults);
	}

	private static Boat inactiveBoatReporting(UUID berth, boolean pluggedIn, int batteryPercent) {
		BoatStatus status = new BoatStatus(batteryPercent, berth, null, true, pluggedIn, List.of());
		return Boat.restore(UUID.randomUUID(), "B-16", BoatType.XL, UUID.randomUUID(), BoatState.INACTIVE, status);
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

	@ParameterizedTest
	@CsvSource({"29, CHARGING", "30, AVAILABLE", "100, AVAILABLE"})
	void activatingAMooredPluggedInBoatChargesItUntilThirtyPercent(int batteryPercent, BoatState expected) {
		Boat boat = inactiveBoatReporting(BERTH, true, batteryPercent);

		Boat activated = boat.activate();

		assertThat(activated.state()).isEqualTo(expected);
		assertThat(activated.status()).isEqualTo(boat.status());
		assertThat(boat.state()).isEqualTo(BoatState.INACTIVE);
	}

	@Test
	void aBoatNotPluggedInIsNotActivated() {
		Boat boat = inactiveBoatReporting(BERTH, false, FULL_BATTERY);

		assertThatThrownBy(boat::activate)
				.isInstanceOf(BoatTransitionRefusedException.class)
				.hasMessageContaining("moored in a berth and plugged in");
	}

	@Test
	void aBoatNotMooredInABerthIsNotActivated() {
		Boat boat = inactiveBoatReporting(null, true, FULL_BATTERY);

		assertThatThrownBy(boat::activate).isInstanceOf(BoatTransitionRefusedException.class);
	}

	@Test
	void aBoatWithoutTelemetryIsNotActivated() {
		Boat boat = Boat.register("B-16", BoatType.XL, UUID.randomUUID());

		assertThatThrownBy(boat::activate).isInstanceOf(BoatTransitionRefusedException.class);
	}

	@ParameterizedTest
	@EnumSource(value = BoatState.class, names = "INACTIVE", mode = EnumSource.Mode.EXCLUDE)
	void onlyAnInactiveBoatIsActivated(BoatState state) {
		Boat boat = Boat.restore(UUID.randomUUID(), "B-16", BoatType.XL, UUID.randomUUID(), state,
				new BoatStatus(FULL_BATTERY, BERTH, null, true, true, List.of()));

		assertThatThrownBy(boat::activate)
				.isInstanceOf(BoatTransitionRefusedException.class)
				.hasMessageContaining(state.name());
	}

	@ParameterizedTest
	@EnumSource(value = BoatState.class, names = {"AVAILABLE", "CHARGING"})
	void anAvailableOrChargingBoatIsDeactivated(BoatState state) {
		BoatStatus status = reading(FULL_BATTERY, List.of());
		Boat boat = Boat.restore(UUID.randomUUID(), "B-16", BoatType.XL, UUID.randomUUID(), state, status);

		Boat deactivated = boat.deactivate();

		assertThat(deactivated.state()).isEqualTo(BoatState.INACTIVE);
		assertThat(deactivated.status()).contains(status);
		assertThat(deactivated.id()).isEqualTo(boat.id());
	}

	@ParameterizedTest
	@EnumSource(value = BoatState.class, names = {"AVAILABLE", "CHARGING"}, mode = EnumSource.Mode.EXCLUDE)
	void onlyAnAvailableOrChargingBoatIsDeactivated(BoatState state) {
		Boat boat = boatIn(state);

		assertThatThrownBy(boat::deactivate)
				.isInstanceOf(BoatTransitionRefusedException.class)
				.hasMessageContaining(state.name());
	}
}
