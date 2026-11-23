package pt.ua.tqs.barkito.fleet.adapter.web;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import pt.ua.tqs.barkito.fleet.application.AvailableBoatService;
import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatType;

/** End-user endpoint to find the boats that can be booked at a dock (US-09). */
@RestController
@RequestMapping("/api/v1/docks/{dockId}/boats")
@Tag(name = "Available boats", description = "Boats an end user can book")
public class AvailableBoatController {

	private final AvailableBoatService availableBoats;

	public AvailableBoatController(AvailableBoatService availableBoats) {
		this.availableBoats = availableBoats;
	}

	@Operation(summary = "Find available boats at a dock",
			description = "US-09: AVAILABLE boats with at least 30 % battery (BR5), ordered by code, optionally of one type.")
	@ApiResponse(responseCode = "200", description = "The available boats; empty when the dock has none")
	@ApiResponse(responseCode = "400", description = "A type other than STANDARD or XL")
	@ApiResponse(responseCode = "404", description = "The dock does not exist")
	@GetMapping
	public List<AvailableBoatResponse> find(@PathVariable UUID dockId,
			@Parameter(description = "STANDARD or XL; all types when absent") @RequestParam(required = false) BoatType type) {
		List<Boat> found = type == null
				? availableBoats.findAvailableAt(dockId)
				: availableBoats.findAvailableAt(dockId, type);
		return found.stream().map(AvailableBoatResponse::from).toList();
	}
}
