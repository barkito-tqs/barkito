package pt.ua.tqs.barkito.maintenance.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import pt.ua.tqs.barkito.fleet.domain.BoatState;

class TicketTypeTest {

  @ParameterizedTest
  @CsvSource({
    "CHARGE_REQUIRED, CHARGE",
    "REPAIR_REQUIRED, REPAIR",
    "TOW_REQUIRED, TOW",
    "RETURN_REQUIRED, RETURN"
  })
  void eachStateThatWaitsForATechnicianOpensItsTicket(BoatState state, TicketType type) {
    assertThat(TicketType.forState(state)).contains(type);
  }

  @ParameterizedTest
  @EnumSource(
      value = BoatState.class,
      names = {"CHARGE_REQUIRED", "REPAIR_REQUIRED", "TOW_REQUIRED", "RETURN_REQUIRED"},
      mode = EnumSource.Mode.EXCLUDE)
  void theOtherStatesOpenNoTicket(BoatState state) {
    assertThat(TicketType.forState(state)).isEmpty();
  }

  @Test
  void everyTypeHasADefaultSlaPolicy() {
    for (TicketType type : TicketType.values()) {
      assertThat(SlaPolicy.defaultFor(type).type()).isEqualTo(type);
    }
  }
}
