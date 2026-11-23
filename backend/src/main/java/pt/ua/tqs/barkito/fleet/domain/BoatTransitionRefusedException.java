package pt.ua.tqs.barkito.fleet.domain;

/** The boat's state or its latest telemetry reading does not allow the requested lifecycle transition. */
public class BoatTransitionRefusedException extends RuntimeException {

	public BoatTransitionRefusedException(String message) {
		super(message);
	}
}
