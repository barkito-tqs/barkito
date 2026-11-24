package pt.ua.tqs.barkito.fleet.domain;

import java.util.UUID;

/** Docks for tests that only need a dock to exist. */
public final class TestDocks {

  /** A location on the canals of Ria Nova. */
  public static final Position CANAL = new Position(40.6405, -8.6538);

  private TestDocks() {}

  public static Dock dock(String name) {
    return new Dock(UUID.randomUUID(), name, null, CANAL);
  }
}
