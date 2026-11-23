package pt.ua.tqs.barkito.fleet.application;

import pt.ua.tqs.barkito.fleet.domain.Dock;

/** A request that would leave a dock with fewer than 1 or more than 10 berths (US-04). */
public class InvalidBerthCountException extends RuntimeException {

  private InvalidBerthCountException(String message) {
    super(message);
  }

  public static InvalidBerthCountException outOfRange(int berthCount) {
    return new InvalidBerthCountException(
        "A dock has from %d to %d berths; got %d."
            .formatted(Dock.MIN_BERTHS, Dock.MAX_BERTHS, berthCount));
  }

  public static InvalidBerthCountException nothingToAdd(int count) {
    return new InvalidBerthCountException(
        "At least %d berth must be added; got %d.".formatted(Dock.MIN_BERTHS, count));
  }

  public static InvalidBerthCountException aboveMaximum(Dock dock, int existing, int count) {
    return new InvalidBerthCountException(
        "%s has %d berths; adding %d would exceed the maximum of %d."
            .formatted(dock.name(), existing, count, Dock.MAX_BERTHS));
  }
}
