package pt.ua.tqs.barkito.fleet.application;

import java.util.UUID;

public class DockNotFoundException extends RuntimeException {

	public DockNotFoundException(UUID dockId) {
		super("Dock " + dockId + " does not exist.");
	}
}
