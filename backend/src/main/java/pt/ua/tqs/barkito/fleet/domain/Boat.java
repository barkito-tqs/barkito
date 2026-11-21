package pt.ua.tqs.barkito.fleet.domain;

import java.util.Objects;
import java.util.UUID;

/** A boat of the fleet, identified by the code painted on its hull. */
public final class Boat {

	private final UUID id;
	private final String code;
	private final BoatType type;
	private final UUID homeDockId;
	private final BoatState state;

	private Boat(UUID id, String code, BoatType type, UUID homeDockId, BoatState state) {
		this.id = Objects.requireNonNull(id);
		this.code = Objects.requireNonNull(code);
		this.type = Objects.requireNonNull(type);
		this.homeDockId = Objects.requireNonNull(homeDockId);
		this.state = Objects.requireNonNull(state);
	}

	/** Lifecycle transition 1: a newly registered boat starts INACTIVE until the operator activates it. */
	public static Boat register(String code, BoatType type, UUID homeDockId) {
		return new Boat(UUID.randomUUID(), code, type, homeDockId, BoatState.INACTIVE);
	}

	public UUID id() {
		return id;
	}

	public String code() {
		return code;
	}

	public BoatType type() {
		return type;
	}

	public UUID homeDockId() {
		return homeDockId;
	}

	public BoatState state() {
		return state;
	}
}
