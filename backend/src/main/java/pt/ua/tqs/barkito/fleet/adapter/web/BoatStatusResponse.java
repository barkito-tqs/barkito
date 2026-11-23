package pt.ua.tqs.barkito.fleet.adapter.web;

import java.util.List;
import java.util.UUID;
import pt.ua.tqs.barkito.fleet.domain.BoatStatus;

/** The latest telemetry reading of a boat. Berth, latitude and longitude are null when unknown. */
public record BoatStatusResponse(
    int batteryPercent,
    UUID berthId,
    Double latitude,
    Double longitude,
    boolean locked,
    boolean pluggedIn,
    List<String> faults) {

  static BoatStatusResponse from(BoatStatus status) {
    Double latitude = status.position() == null ? null : status.position().latitude();
    Double longitude = status.position() == null ? null : status.position().longitude();
    return new BoatStatusResponse(
        status.batteryPercent(),
        status.berthId(),
        latitude,
        longitude,
        status.locked(),
        status.pluggedIn(),
        status.faults());
  }
}
