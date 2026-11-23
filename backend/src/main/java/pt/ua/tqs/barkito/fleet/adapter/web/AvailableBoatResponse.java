package pt.ua.tqs.barkito.fleet.adapter.web;

import java.util.UUID;

import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatStatus;
import pt.ua.tqs.barkito.fleet.domain.BoatType;

/** A bookable boat as the end user sees it: its type and battery level (US-09). */
public record AvailableBoatResponse(UUID id, String code, BoatType type, int batteryPercent) {

	/** Only for bookable boats, which always have a telemetry reading. */
	static AvailableBoatResponse from(Boat boat) {
		int battery = boat.status().map(BoatStatus::batteryPercent).orElseThrow();
		return new AvailableBoatResponse(boat.id(), boat.code(), boat.type(), battery);
	}
}
