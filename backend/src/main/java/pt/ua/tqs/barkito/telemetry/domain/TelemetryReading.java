package pt.ua.tqs.barkito.telemetry.domain;

import pt.ua.tqs.barkito.fleet.domain.BoatStatus;

/** A validated telemetry reading: the code of the boat that sent it and the status it reported. */
public record TelemetryReading(String boatCode, BoatStatus status) {}
