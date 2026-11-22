package pt.ua.tqs.barkito.fleet.adapter.web;

import java.util.UUID;
import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatState;
import pt.ua.tqs.barkito.fleet.domain.BoatType;

public record BoatResponse(
    UUID id,
    String code,
    BoatType type,
    BoatState state,
    UUID homeDockId,
    BoatStatusResponse status) {

  static BoatResponse from(Boat boat) {
    return new BoatResponse(
        boat.id(),
        boat.code(),
        boat.type(),
        boat.state(),
        boat.homeDockId(),
        boat.status().map(BoatStatusResponse::from).orElse(null));
  }
}
