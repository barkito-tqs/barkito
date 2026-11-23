package pt.ua.tqs.barkito.fleet.domain;

import java.util.UUID;

/**
 * A dock where trips start and end. Only what boat registration needs for now; docks and their
 * berths are registered in US-04 (BRK-43).
 */
public record Dock(UUID id, String name) {}
