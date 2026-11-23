package pt.ua.tqs.barkito.fleet.adapter.web;

import java.util.UUID;
import pt.ua.tqs.barkito.fleet.domain.Berth;

public record BerthResponse(UUID id, int number, boolean inService) {

  static BerthResponse from(Berth berth) {
    return new BerthResponse(berth.id(), berth.number(), berth.inService());
  }
}
