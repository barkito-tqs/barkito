package pt.ua.tqs.barkito.fleet.adapter.web;

import java.util.List;
import java.util.UUID;
import pt.ua.tqs.barkito.fleet.application.DockDetails;
import pt.ua.tqs.barkito.fleet.domain.Dock;

/** A dock with its physical berths and how many of them are in service. */
public record DockResponse(
    UUID id,
    String name,
    String district,
    double latitude,
    double longitude,
    int berthCount,
    long berthsInService,
    List<BerthResponse> berths) {

  static DockResponse from(DockDetails details) {
    Dock dock = details.dock();
    return new DockResponse(
        dock.id(),
        dock.name(),
        dock.district(),
        dock.position().latitude(),
        dock.position().longitude(),
        details.berths().size(),
        details.berthsInService(),
        details.berths().stream().map(BerthResponse::from).toList());
  }
}
