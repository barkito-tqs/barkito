package pt.ua.tqs.barkito.fleet.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * A numbered berth of a dock, where one boat moors and charges. Every berth has a charger.
 * Berths are in service by default (US-04); the municipality takes them in and out of service (US-27).
 */
public record Berth(UUID id, UUID dockId, int number, boolean inService) {

	public Berth {
		Objects.requireNonNull(id);
		Objects.requireNonNull(dockId);
		if (number < 1) {
			throw new IllegalArgumentException("Berth numbers start at 1, got " + number);
		}
	}

	/** A new berth of the dock, in service. */
	public static Berth create(UUID dockId, int number) {
		return new Berth(UUID.randomUUID(), dockId, number, true);
	}
}
