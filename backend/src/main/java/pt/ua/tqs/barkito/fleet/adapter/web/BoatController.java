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

import jakarta.validation.Valid;
import pt.ua.tqs.barkito.fleet.application.BoatService;
import pt.ua.tqs.barkito.fleet.domain.Boat;

/** Operator endpoints for the fleet. Access by role comes with US-02. */
@RestController
@RequestMapping("/api/boats")
public class BoatController {

	private final BoatService boatService;

	public BoatController(BoatService boatService) {
		this.boatService = boatService;
	}

	@PostMapping
	public ResponseEntity<BoatResponse> register(@Valid @RequestBody RegisterBoatRequest request) {
		Boat boat = boatService.register(request.code(), request.type(), request.homeDockId());
		return ResponseEntity.created(URI.create("/api/boats/" + boat.id())).body(BoatResponse.from(boat));
	}

	@GetMapping
	public List<BoatResponse> list() {
		return boatService.findAll().stream().map(BoatResponse::from).toList();
	}

	@GetMapping("/{id}")
	public ResponseEntity<BoatResponse> get(@PathVariable UUID id) {
		return ResponseEntity.of(boatService.findById(id).map(BoatResponse::from));
	}
}
