package pt.ua.tqs.barkito.telemetry.adapter.simulator;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import pt.ua.tqs.barkito.telemetry.domain.TelemetrySource;

/** An in-process {@link TelemetrySource}: whatever is published is delivered synchronously to every subscriber. */
@Component
public class SimulatedTelemetrySource implements TelemetrySource {

	private final List<Consumer<String>> subscribers = new CopyOnWriteArrayList<>();

	@Override
	public void subscribe(Consumer<String> payload) {
		subscribers.add(payload);
	}

	/** Delivers the payload to every subscriber. */
	public void publish(String payload) {
		subscribers.forEach(subscriber -> subscriber.accept(payload));
	}
}
