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
}
