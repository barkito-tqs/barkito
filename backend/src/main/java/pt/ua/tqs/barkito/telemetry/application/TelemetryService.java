package pt.ua.tqs.barkito.telemetry.application;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import pt.ua.tqs.barkito.fleet.application.BoatService;
import pt.ua.tqs.barkito.telemetry.domain.TelemetrySource;

/** Applies each telemetry payload delivered by the {@link TelemetrySource} to the boat that sent it. */
@Service
public class TelemetryService {

	private static final Logger LOG = LoggerFactory.getLogger(TelemetryService.class);

	private final TelemetrySource source;
	private final TelemetryReadingParser parser;
	private final BoatService boats;

	public TelemetryService(TelemetrySource source, TelemetryReadingParser parser, BoatService boats) {
		this.source = source;
		this.parser = parser;
		this.boats = boats;
	}

	@PostConstruct
	void start() {
		source.subscribe(this::accept);
	}

	/** Parses the payload and records the reading; an invalid payload or an unknown boat is logged and dropped. */
	void accept(String payload) {
		parser.parse(payload).ifPresent(reading -> {
			if (boats.recordStatus(reading.boatCode(), reading.status()).isEmpty()) {
				LOG.warn("Ignoring telemetry for unknown boat {}", reading.boatCode());
			}
		});
	}
}
