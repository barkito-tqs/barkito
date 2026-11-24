package pt.ua.tqs.barkito.fleet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DockTest {

  private static final Position OLD_MARKET = new Position(40.641817, -8.66465);

  @Test
  void aNewDockKeepsItsNameDistrictAndLocation() {
    Dock dock = Dock.register("Old Market", "Mercado", OLD_MARKET);

    assertThat(dock.id()).isNotNull();
    assertThat(dock.name()).isEqualTo("Old Market");
    assertThat(dock.district()).isEqualTo("Mercado");
    assertThat(dock.position()).isEqualTo(OLD_MARKET);
  }

  @Test
  void theNameAndDistrictAreTrimmed() {
    Dock dock = Dock.register("  Old Market ", " Mercado ", OLD_MARKET);

    assertThat(dock.name()).isEqualTo("Old Market");
    assertThat(dock.district()).isEqualTo("Mercado");
  }

  @Test
  void theDistrictIsOptional() {
    assertThat(Dock.register("Old Market", null, OLD_MARKET).district()).isNull();
    assertThat(Dock.register("Old Market", "  ", OLD_MARKET).district()).isNull();
  }

  @Test
  void aDockNeedsANameAndALocation() {
    assertThatThrownBy(() -> Dock.register(" ", null, OLD_MARKET))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Dock.register("Old Market", null, null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void aDockHasFrom1To10Berths() {
    assertThat(Dock.MIN_BERTHS).isEqualTo(1);
    assertThat(Dock.MAX_BERTHS).isEqualTo(10);
    assertThat(Dock.allowsBerthCount(0)).isFalse();
    assertThat(Dock.allowsBerthCount(1)).isTrue();
    assertThat(Dock.allowsBerthCount(10)).isTrue();
    assertThat(Dock.allowsBerthCount(11)).isFalse();
  }
}
