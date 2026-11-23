package pt.ua.tqs.barkito.fleet.adapter.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pt.ua.tqs.barkito.fleet.application.DockDetails;
import pt.ua.tqs.barkito.fleet.application.DockService;
import pt.ua.tqs.barkito.fleet.domain.Position;

/** Operator endpoints for docks and their berths (US-04). Access by role comes with US-02. */
@RestController
@RequestMapping("/api/v1/docks")
@Tag(name = "Docks", description = "Docks and their berths")
public class DockController {

  private final DockService dockService;

  public DockController(DockService dockService) {
    this.dockService = dockService;
  }

  @Operation(
      summary = "Register a dock",
      description =
          "US-04: a dock with a unique name, a location and 1 to 10 berths, numbered from 1 and"
              + " in service.")
  @ApiResponse(responseCode = "201", description = "Dock registered with its berths")
  @ApiResponse(
      responseCode = "400",
      description = "Missing or too long name, district too long, or a location out of range")
  @ApiResponse(responseCode = "409", description = "A dock with this name is already registered")
  @ApiResponse(responseCode = "422", description = "Fewer than 1 or more than 10 berths")
  @PostMapping
  public ResponseEntity<DockResponse> register(@Valid @RequestBody RegisterDockRequest request) {
    DockDetails details =
        dockService.register(
            request.name(),
            request.district(),
            new Position(request.latitude(), request.longitude()),
            request.berthCount());
    return ResponseEntity.created(URI.create("/api/v1/docks/" + details.dock().id()))
        .body(DockResponse.from(details));
  }

  @Operation(summary = "List the docks", description = "Every dock with its berths, by name.")
  @GetMapping
  public List<DockResponse> list() {
    return dockService.findAll().stream().map(DockResponse::from).toList();
  }

  @Operation(summary = "Get a dock with its berths")
  @ApiResponse(responseCode = "200", description = "The dock")
  @ApiResponse(responseCode = "404", description = "No dock with this id")
  @GetMapping("/{id}")
  public ResponseEntity<DockResponse> get(@PathVariable UUID id) {
    return ResponseEntity.of(dockService.findById(id).map(DockResponse::from));
  }

  @Operation(
      summary = "Add berths to a dock",
      description =
          "BRK-321: the new berths continue the dock's numbering and are in service; a dock has at"
              + " most 10 berths.")
  @ApiResponse(responseCode = "201", description = "The dock with all its berths")
  @ApiResponse(responseCode = "400", description = "Missing count")
  @ApiResponse(responseCode = "404", description = "No dock with this id")
  @ApiResponse(
      responseCode = "422",
      description = "Fewer than 1 berth, or more than 10 berths in total")
  @PostMapping("/{id}/berths")
  @ResponseStatus(HttpStatus.CREATED)
  public DockResponse addBerths(
      @PathVariable UUID id, @Valid @RequestBody AddBerthsRequest request) {
    return DockResponse.from(dockService.addBerths(id, request.count()));
  }
}
