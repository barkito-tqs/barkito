package pt.ua.tqs.barkito.fleet.adapter.web;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pt.ua.tqs.barkito.fleet.application.BoatNotFoundException;
import pt.ua.tqs.barkito.fleet.application.BoatService;
import pt.ua.tqs.barkito.fleet.application.DockNotFoundException;
import pt.ua.tqs.barkito.fleet.application.DuplicateBoatCodeException;
import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatState;
import pt.ua.tqs.barkito.fleet.domain.BoatStatus;
import pt.ua.tqs.barkito.fleet.domain.BoatTransitionRefusedException;
import pt.ua.tqs.barkito.fleet.domain.BoatType;
import pt.ua.tqs.barkito.fleet.domain.Position;

@WebMvcTest(BoatController.class)
class BoatControllerTest {

	private static final UUID DOCK = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BoatService boatService;

	private static String body(String code, String type, Object dock) {
		return """
				{"code": "%s", "type": "%s", "homeDockId": "%s"}
				""".formatted(code, type, dock);
	}

	@Test
	void registeringABoatReturns201WithTheInactiveBoat() throws Exception {
		Boat boat = Boat.register("B-16", BoatType.STANDARD, DOCK);
		when(boatService.register("B-16", BoatType.STANDARD, DOCK)).thenReturn(boat);

		mockMvc.perform(post("/api/v1/boats").contentType(MediaType.APPLICATION_JSON).content(body("B-16", "STANDARD", DOCK)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/v1/boats/" + boat.id()))
				.andExpect(jsonPath("$.code").value("B-16"))
				.andExpect(jsonPath("$.type").value("STANDARD"))
				.andExpect(jsonPath("$.state").value("INACTIVE"))
				.andExpect(jsonPath("$.homeDockId").value(DOCK.toString()));
	}

	@Test
	void aDuplicateCodeIsRefusedWith409() throws Exception {
		when(boatService.register("B-16", BoatType.XL, DOCK)).thenThrow(new DuplicateBoatCodeException("B-16"));

		mockMvc.perform(post("/api/v1/boats").contentType(MediaType.APPLICATION_JSON).content(body("B-16", "XL", DOCK)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("A boat with code B-16 is already registered."));
	}

	@Test
	void aHomeDockThatDoesNotExistIsRefusedWith404() throws Exception {
		when(boatService.register("B-16", BoatType.XL, DOCK)).thenThrow(new DockNotFoundException(DOCK));

		mockMvc.perform(post("/api/v1/boats").contentType(MediaType.APPLICATION_JSON).content(body("B-16", "XL", DOCK)))
				.andExpect(status().isNotFound());
	}

	@Test
	void aTypeOtherThanStandardOrXlIsRefusedWith400() throws Exception {
		mockMvc.perform(post("/api/v1/boats").contentType(MediaType.APPLICATION_JSON).content(body("B-16", "KAYAK", DOCK)))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(boatService);
	}

	@Test
	void aBlankCodeIsRefusedWith400() throws Exception {
		mockMvc.perform(post("/api/v1/boats").contentType(MediaType.APPLICATION_JSON).content(body(" ", "XL", DOCK)))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(boatService);
	}

	@Test
	void theFleetListShowsRegisteredBoats() throws Exception {
		when(boatService.findAll()).thenReturn(List.of(Boat.register("B-16", BoatType.XL, DOCK)));

		mockMvc.perform(get("/api/v1/boats"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].code").value("B-16"))
				.andExpect(jsonPath("$[0].state").value("INACTIVE"));
	}

	@Test
	void anUnknownBoatIdReturns404() throws Exception {
		UUID id = UUID.randomUUID();
		when(boatService.findById(id)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/boats/" + id)).andExpect(status().isNotFound());
	}

	@Test
	void aKnownBoatIdReturnsTheBoat() throws Exception {
		Boat boat = Boat.register("B-16", BoatType.XL, DOCK);
		when(boatService.findById(any(UUID.class))).thenReturn(Optional.of(boat));

		mockMvc.perform(get("/api/v1/boats/" + boat.id()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("B-16"));
	}

	@Test
	void aBoatWithAStatusShowsIt() throws Exception {
		UUID berth = UUID.randomUUID();
		Boat boat = Boat.restore(UUID.randomUUID(), "B-16", BoatType.XL, DOCK, BoatState.CHARGING,
				new BoatStatus(64, berth, new Position(40.64, -8.65), true, true, List.of("MOTOR_OVERHEAT")));
		when(boatService.findById(any(UUID.class))).thenReturn(Optional.of(boat));

		mockMvc.perform(get("/api/v1/boats/" + boat.id()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status.batteryPercent").value(64))
				.andExpect(jsonPath("$.status.berthId").value(berth.toString()))
				.andExpect(jsonPath("$.status.latitude").value(40.64))
				.andExpect(jsonPath("$.status.longitude").value(-8.65))
				.andExpect(jsonPath("$.status.locked").value(true))
				.andExpect(jsonPath("$.status.pluggedIn").value(true))
				.andExpect(jsonPath("$.status.faults[0]").value("MOTOR_OVERHEAT"));
	}

	@Test
	void aBoatWithoutAStatusHasNoStatus() throws Exception {
		Boat boat = Boat.register("B-16", BoatType.XL, DOCK);
		when(boatService.findById(any(UUID.class))).thenReturn(Optional.of(boat));

		mockMvc.perform(get("/api/v1/boats/" + boat.id()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").doesNotExist());
	}

	@Test
	void activatingABoatReturnsItInItsNewState() throws Exception {
		Boat boat = Boat.restore(UUID.randomUUID(), "B-16", BoatType.XL, DOCK, BoatState.CHARGING, null);
		when(boatService.activate(boat.id())).thenReturn(boat);

		mockMvc.perform(post("/api/v1/boats/{id}/activation", boat.id()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("B-16"))
				.andExpect(jsonPath("$.state").value("CHARGING"));
	}

	@Test
	void aRefusedActivationReturns409() throws Exception {
		UUID id = UUID.randomUUID();
		when(boatService.activate(id)).thenThrow(new BoatTransitionRefusedException(
				"B-16 must be moored in a berth and plugged in before it is activated."));

		mockMvc.perform(post("/api/v1/boats/{id}/activation", id))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("B-16 must be moored in a berth and plugged in before it is activated."));
	}

	@Test
	void activatingAnUnknownBoatReturns404() throws Exception {
		UUID id = UUID.randomUUID();
		when(boatService.activate(id)).thenThrow(new BoatNotFoundException(id));

		mockMvc.perform(post("/api/v1/boats/{id}/activation", id)).andExpect(status().isNotFound());
	}

	@Test
	void deactivatingABoatReturnsItInactive() throws Exception {
		Boat boat = Boat.register("B-16", BoatType.XL, DOCK);
		when(boatService.deactivate(boat.id())).thenReturn(boat);

		mockMvc.perform(post("/api/v1/boats/{id}/deactivation", boat.id()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.state").value("INACTIVE"));
	}

	@Test
	void aRefusedDeactivationReturns409() throws Exception {
		UUID id = UUID.randomUUID();
		when(boatService.deactivate(id)).thenThrow(new BoatTransitionRefusedException("B-16 is IN_USE."));

		mockMvc.perform(post("/api/v1/boats/{id}/deactivation", id)).andExpect(status().isConflict());
	}
}
