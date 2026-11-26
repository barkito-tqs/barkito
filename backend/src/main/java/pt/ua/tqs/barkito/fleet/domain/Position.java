package pt.ua.tqs.barkito.fleet.domain;

/** A geographic position in decimal degrees. */
public record Position(double latitude, double longitude) {

  /** Mean radius of the Earth, for the haversine formula. */
  private static final double EARTH_RADIUS_METRES = 6_371_000;

  /** The great-circle distance to another position, in metres (haversine formula). */
  public double distanceInMetresTo(Position other) {
    double deltaLatitude = Math.toRadians(other.latitude - latitude);
    double deltaLongitude = Math.toRadians(other.longitude - longitude);
    double a =
        Math.pow(Math.sin(deltaLatitude / 2), 2)
            + Math.cos(Math.toRadians(latitude))
                * Math.cos(Math.toRadians(other.latitude))
                * Math.pow(Math.sin(deltaLongitude / 2), 2);
    return 2 * EARTH_RADIUS_METRES * Math.asin(Math.sqrt(a));
  }
}
