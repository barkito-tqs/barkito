package pt.ua.tqs.barkito.fleet.adapter.web;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import pt.ua.tqs.barkito.fleet.application.BoatService;
import pt.ua.tqs.barkito.fleet.domain.Boat;

/** Operator endpoints for the fleet. Access by role comes with US-02. */
@RestController
@RequestMapping("/api/v1/boats")
@Tag(name = "Fleet", description = "Boats of the fleet")
public class BoatController {

	private final BoatService boatService;

	public BoatController(BoatService boatService) {
		this.boatService = boatService;
	}

	@Operation(summary = "Register a boat", description = "US-03: the new boat starts INACTIVE and joins the fleet list.")
	@ApiResponse(responseCode = "201", description = "Boat registered")
	@ApiResponse(responseCode = "400", description = "Missing code, code too long, or a type other than STANDARD or XL")
	@ApiResponse(responseCode = "404", description = "The home dock does not exist")
	@ApiResponse(responseCode = "409", description = "A boat with this code is already registered")
	@PostMapping
	public ResponseEntity<BoatResponse> register(@Valid @RequestBody RegisterBoatRequest request) {
		Boat boat = boatService.register(request.code(), request.type(), request.homeDockId());
		return ResponseEntity.created(URI.create("/api/v1/boats/" + boat.id())).body(BoatResponse.from(boat));
	}

	@Operation(summary = "List the fleet", description = "The operator's fleet list, ordered by code.")
	@GetMapping
	public List<BoatResponse> list() {
		return boatService.findAll().stream().map(BoatResponse::from).toList();
	}

	@Operation(summary = "Activate a boat",
			description = "US-05: an INACTIVE boat moored in a berth and plugged in becomes CHARGING, "
					+ "or AVAILABLE when its battery is already at 30 % or more (BR19, BR5).")
	@ApiResponse(responseCode = "200", description = "The boat in its new state")
	@ApiResponse(responseCode = "404", description = "No boat with this id")
	@ApiResponse(responseCode = "409", description = "The boat is not INACTIVE, or not moored in a berth and plugged in")
	@PostMapping("/{id}/activation")
	public BoatResponse activate(@PathVariable UUID id) {
		return BoatResponse.from(boatService.activate(id));
	}

	@Operation(summary = "Deactivate a boat",
			description = "US-05: an AVAILABLE or CHARGING boat becomes INACTIVE and is hidden from users (BR19).")
	@ApiResponse(responseCode = "200", description = "The boat, now INACTIVE")
	@ApiResponse(responseCode = "404", description = "No boat with this id")
	@ApiResponse(responseCode = "409", description = "The boat is reserved, on a trip or in another state")
	@PostMapping("/{id}/deactivation")
	public BoatResponse deactivate(@PathVariable UUID id) {
		return BoatResponse.from(boatService.deactivate(id));
	}

	@Operation(summary = "Get a boat")
	@ApiResponse(responseCode = "200", description = "The boat")
	@ApiResponse(responseCode = "404", description = "No boat with this id")
	@GetMapping("/{id}")
	public ResponseEntity<BoatResponse> get(@PathVariable UUID id) {
		return ResponseEntity.of(boatService.findById(id).map(BoatResponse::from));
	}
}
