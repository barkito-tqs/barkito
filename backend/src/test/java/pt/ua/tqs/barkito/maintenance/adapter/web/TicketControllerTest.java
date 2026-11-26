package pt.ua.tqs.barkito.maintenance.adapter.web;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pt.ua.tqs.barkito.accounts.adapter.security.TestRoles.as;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.ua.tqs.barkito.accounts.adapter.security.SecuredWebMvcTestConfig;
import pt.ua.tqs.barkito.accounts.domain.Role;
import pt.ua.tqs.barkito.maintenance.application.TicketDetails;
import pt.ua.tqs.barkito.maintenance.application.TicketService;
import pt.ua.tqs.barkito.maintenance.domain.Ticket;
import pt.ua.tqs.barkito.maintenance.domain.TicketStatus;
import pt.ua.tqs.barkito.maintenance.domain.TicketType;

@WebMvcTest(TicketController.class)
@Import(SecuredWebMvcTestConfig.class)
class TicketControllerTest {

  private static final Instant OPENED = Instant.parse("2026-11-26T17:20:00Z");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TicketService ticketService;

  @Test
  void theOperatorSeesEachTicketWithItsTechnicianAndDeadlines() throws Exception {
    Ticket tow =
        new Ticket(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TicketType.TOW,
            TicketStatus.OPEN,
            UUID.randomUUID(),
            OPENED,
            Instant.parse("2026-11-26T19:20:00Z"),
            Instant.parse("2026-11-26T17:50:00Z"));
    when(ticketService.findAll()).thenReturn(List.of(new TicketDetails(tow, "Tiago Ferreira")));

    mockMvc
        .perform(get("/api/v1/tickets").with(as(Role.OPERATOR)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(tow.id().toString()))
        .andExpect(jsonPath("$[0].boatId").value(tow.boatId().toString()))
        .andExpect(jsonPath("$[0].type").value("TOW"))
        .andExpect(jsonPath("$[0].status").value("OPEN"))
        .andExpect(jsonPath("$[0].technicianId").value(tow.technicianId().toString()))
        .andExpect(jsonPath("$[0].technicianName").value("Tiago Ferreira"))
        .andExpect(jsonPath("$[0].openedAt").value("2026-11-26T17:20:00Z"))
        .andExpect(jsonPath("$[0].dueAt").value("2026-11-26T19:20:00Z"))
        .andExpect(jsonPath("$[0].onSiteDueAt").value("2026-11-26T17:50:00Z"));
  }

  @Test
  void anUnassignedTicketHasNoTechnician() throws Exception {
    Ticket charge =
        new Ticket(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TicketType.CHARGE,
            TicketStatus.OPEN,
            null,
            OPENED,
            OPENED,
            null);
    when(ticketService.findAll()).thenReturn(List.of(new TicketDetails(charge, null)));

    mockMvc
        .perform(get("/api/v1/tickets").with(as(Role.OPERATOR)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].technicianId").doesNotExist())
        .andExpect(jsonPath("$[0].onSiteDueAt").doesNotExist());
  }
}
