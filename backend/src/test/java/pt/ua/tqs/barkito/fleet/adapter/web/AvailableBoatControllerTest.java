package pt.ua.tqs.barkito.fleet.adapter.web;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pt.ua.tqs.barkito.fleet.application.AvailableBoatService;
import pt.ua.tqs.barkito.fleet.application.DockNotFoundException;
import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatState;
import pt.ua.tqs.barkito.fleet.domain.BoatStatus;
import pt.ua.tqs.barkito.fleet.domain.BoatType;

@WebMvcTest(AvailableBoatController.class)
class AvailableBoatControllerTest {

	private static final UUID DOCK = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
	private static final String URL = "/api/v1/docks/{dockId}/boats";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AvailableBoatService availableBoats;

	private static Boat available(String code, BoatType type, int battery) {
		BoatStatus reading = new BoatStatus(battery, UUID.randomUUID(), null, true, false, List.of());
		return Boat.restore(UUID.randomUUID(), code, type, DOCK, BoatState.AVAILABLE, reading);
	}

	@Test
	void listsTheAvailableBoatsWithTheirTypeAndBattery() throws Exception {
		when(availableBoats.findAvailableAt(DOCK)).thenReturn(List.of(available("B-01", BoatType.XL, 64)));

		mockMvc.perform(get(URL, DOCK))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].code").value("B-01"))
				.andExpect(jsonPath("$[0].type").value("XL"))
				.andExpect(jsonPath("$[0].batteryPercent").value(64));
	}

	@Test
	void filtersByTypeWhenGiven() throws Exception {
		when(availableBoats.findAvailableAt(DOCK, BoatType.STANDARD))
				.thenReturn(List.of(available("B-02", BoatType.STANDARD, 80)));

		mockMvc.perform(get(URL, DOCK).param("type", "STANDARD"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].code").value("B-02"));
	}

	@Test
	void anUnknownDockReturns404() throws Exception {
		when(availableBoats.findAvailableAt(DOCK)).thenThrow(new DockNotFoundException(DOCK));

		mockMvc.perform(get(URL, DOCK)).andExpect(status().isNotFound());
	}

	@Test
	void aTypeOtherThanStandardOrXlReturns400() throws Exception {
		mockMvc.perform(get(URL, DOCK).param("type", "KAYAK")).andExpect(status().isBadRequest());
		verifyNoInteractions(availableBoats);
	}
}
