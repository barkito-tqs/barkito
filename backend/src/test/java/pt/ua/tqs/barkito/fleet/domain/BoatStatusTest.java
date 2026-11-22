package pt.ua.tqs.barkito.fleet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BoatStatusTest {

  private static BoatStatus withBattery(int batteryPercent) {
    return new BoatStatus(batteryPercent, null, null, true, false, List.of());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 100})
  void acceptsBatteryAtTheLimits(int batteryPercent) {
    assertThat(withBattery(batteryPercent).batteryPercent()).isEqualTo(batteryPercent);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 101})
  void rejectsBatteryOutsideZeroToOneHundred(int batteryPercent) {
    assertThatThrownBy(() -> withBattery(batteryPercent))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void reportsFaultsOnlyWhenThereAreAny() {
    BoatStatus faulty = new BoatStatus(50, null, null, true, false, List.of("MOTOR"));

    assertThat(faulty.hasFault()).isTrue();
    assertThat(withBattery(50).hasFault()).isFalse();
  }
}
