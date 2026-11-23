package pt.ua.tqs.barkito.fleet.domain;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** A boat of the fleet, identified by the code painted on its hull. */
public final class Boat {

	/** Business rule BR5: the minimum battery level for a boat to be bookable. */
	public static final int MIN_BOOKABLE_BATTERY_PERCENT = 30;

	private final UUID id;
	private final String code;
	private final BoatType type;
	private final UUID homeDockId;
	private final BoatState state;
	private final BoatStatus status;

	private Boat(UUID id, String code, BoatType type, UUID homeDockId, BoatState state, BoatStatus status) {
		this.id = Objects.requireNonNull(id);
		this.code = Objects.requireNonNull(code);
		this.type = Objects.requireNonNull(type);
		this.homeDockId = Objects.requireNonNull(homeDockId);
		this.state = Objects.requireNonNull(state);
		this.status = status;
	}

	/** Lifecycle transition 1: a newly registered boat starts INACTIVE until the operator activates it. */
	public static Boat register(String code, BoatType type, UUID homeDockId) {
		return new Boat(UUID.randomUUID(), code, type, homeDockId, BoatState.INACTIVE, null);
	}

	/** Rebuilds a boat in a known state, for persistence and tests; the status is null until the first reading. */
	public static Boat restore(
			UUID id, String code, BoatType type, UUID homeDockId, BoatState state, BoatStatus status) {
		return new Boat(id, code, type, homeDockId, state, status);
	}

	/**
	 * Stores a telemetry reading and applies the lifecycle transitions it triggers, returning a new boat.
	 *
	 * <p>Transition 15: a CHARGING boat whose battery reaches {@link #MIN_BOOKABLE_BATTERY_PERCENT} becomes
	 * AVAILABLE. Transitions 16 and 18: a fault on an AVAILABLE or CHARGING boat makes it REPAIR_REQUIRED, and
	 * the fault wins when both happen in one reading. Any other state is kept and only the reading is stored.
	 */
	public Boat applyStatus(BoatStatus newStatus) {
		Objects.requireNonNull(newStatus);
		return new Boat(id, code, type, homeDockId, nextState(newStatus), newStatus);
	}

	/**
	 * Lifecycle transition 2: the operator puts an INACTIVE boat into service. BR19: the latest reading must
	 * show it moored in a berth and plugged in. The boat becomes CHARGING and, like every boat that arrives
	 * plugged in, leaves for AVAILABLE as soon as its battery is at {@link #MIN_BOOKABLE_BATTERY_PERCENT}.
	 */
	public Boat activate() {
		if (state != BoatState.INACTIVE) {
			throw new BoatTransitionRefusedException(
					"%s is %s; only an INACTIVE boat can be activated.".formatted(code, state));
		}
		if (status == null || status.berthId() == null || !status.pluggedIn()) {
			throw new BoatTransitionRefusedException(
					"%s must be moored in a berth and plugged in before it is activated.".formatted(code));
		}
		return new Boat(id, code, type, homeDockId, BoatState.CHARGING, status).applyStatus(status);
	}

	/**
	 * Lifecycle transitions 3 and 4: the operator takes an AVAILABLE or CHARGING boat out of service. BR19: a
	 * boat that is RESERVED or IN_USE belongs to a user and is not deactivated.
	 */
	public Boat deactivate() {
		if (state != BoatState.AVAILABLE && state != BoatState.CHARGING) {
			throw new BoatTransitionRefusedException(
					"%s is %s; only an AVAILABLE or CHARGING boat can be deactivated.".formatted(code, state));
		}
		return new Boat(id, code, type, homeDockId, BoatState.INACTIVE, status);
	}

	private BoatState nextState(BoatStatus newStatus) {
		boolean canFail = state == BoatState.AVAILABLE || state == BoatState.CHARGING;
		if (canFail && newStatus.hasFault()) {
			return BoatState.REPAIR_REQUIRED;
		}
		if (state == BoatState.CHARGING && newStatus.batteryPercent() >= MIN_BOOKABLE_BATTERY_PERCENT) {
			return BoatState.AVAILABLE;
		}
		return state;
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

	public Optional<BoatStatus> status() {
		return Optional.ofNullable(status);
	}
}
