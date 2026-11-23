package pt.ua.tqs.barkito.fleet.application;

import java.util.UUID;

public class BoatNotFoundException extends RuntimeException {

  public BoatNotFoundException(UUID boatId) {
    super("Boat " + boatId + " does not exist.");
  }
}
