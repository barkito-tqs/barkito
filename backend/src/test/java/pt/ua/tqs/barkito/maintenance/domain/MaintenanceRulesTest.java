package pt.ua.tqs.barkito.maintenance.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MaintenanceRulesTest {

  @Test
  void theDefaultsAreFiveOpenTicketsTheDefaultHoursAndTheBr18Slas() {
    MaintenanceRules rules = MaintenanceRules.DEFAULT;

    assertThat(rules.maxOpenTicketsPerTechnician()).isEqualTo(5);
    assertThat(rules.operatingHours()).isEqualTo(OperatingHours.DEFAULT);
    for (TicketType type : TicketType.values()) {
      assertThat(rules.slaPolicyFor(type)).isEqualTo(SlaPolicy.DEFAULTS.get(type));
    }
  }

  @Test
  void everyTicketTypeNeedsAnSlaPolicy() {
    Map<TicketType, SlaPolicy> policies = new EnumMap<>(SlaPolicy.DEFAULTS);
    policies.remove(TicketType.TOW);

    assertThatThrownBy(() -> new MaintenanceRules(5, OperatingHours.DEFAULT, policies))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("TOW");
  }

  @Test
  void aTechnicianMustBeAbleToHoldAtLeastOneTicket() {
    assertThatThrownBy(() -> new MaintenanceRules(0, OperatingHours.DEFAULT, SlaPolicy.DEFAULTS))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(new MaintenanceRules(1, OperatingHours.DEFAULT, SlaPolicy.DEFAULTS))
        .extracting(MaintenanceRules::maxOpenTicketsPerTechnician)
        .isEqualTo(1);
  }
}
