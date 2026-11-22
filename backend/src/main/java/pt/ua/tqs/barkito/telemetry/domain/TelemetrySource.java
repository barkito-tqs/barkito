package pt.ua.tqs.barkito.telemetry.domain;

import java.util.function.Consumer;

/** Where raw telemetry payloads (JSON text) come from. */
public interface TelemetrySource {

	/** Registers a subscriber that is handed every payload this source delivers. */
	void subscribe(Consumer<String> payload);
}
