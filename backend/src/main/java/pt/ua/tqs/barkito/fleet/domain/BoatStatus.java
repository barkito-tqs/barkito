package pt.ua.tqs.barkito.fleet.domain;

import java.util.List;
import java.util.UUID;

/**
 * The latest telemetry reading of a boat. The berth and the position may be unknown (null), for
 * example when the boat is not docked or has no GPS fix.
 */
public record BoatStatus(
		int batteryPercent,
		UUID berthId,
		Position position,
		boolean locked,
		boolean pluggedIn,
		List<String> faults) {

	private static final int MIN_BATTERY_PERCENT = 0;
	private static final int MAX_BATTERY_PERCENT = 100;

	public BoatStatus {
		if (batteryPercent < MIN_BATTERY_PERCENT || batteryPercent > MAX_BATTERY_PERCENT) {
			throw new IllegalArgumentException("batteryPercent must be between 0 and 100: " + batteryPercent);
		}
		faults = List.copyOf(faults);
	}

	public boolean hasFault() {
		return !faults.isEmpty();
	}
}
