package pt.ua.tqs.barkito.maintenance.adapter.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.ua.tqs.barkito.maintenance.application.TicketService;

/** Operator endpoints for maintenance tickets (US-21). Tickets are opened by the fleet's events. */
@RestController
@RequestMapping("/api/v1/tickets")
@Tag(name = "Maintenance", description = "Tickets for Field Technicians")
public class TicketController {

  private final TicketService ticketService;

  public TicketController(TicketService ticketService) {
    this.ticketService = ticketService;
  }

  @Operation(
      summary = "List the tickets",
      description =
          "US-21: every ticket, ordered by SLA deadline, with its technician (BR17) and deadlines"
              + " (BR18).")
  @GetMapping
  public List<TicketResponse> list() {
    return ticketService.findAll().stream().map(TicketResponse::from).toList();
  }
}
