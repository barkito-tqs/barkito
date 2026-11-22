package pt.ua.tqs.barkito.telemetry.adapter.simulator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimulatedTelemetrySourceTest {

  @Test
  void deliversThePayloadToEverySubscriber() {
    SimulatedTelemetrySource source = new SimulatedTelemetrySource();
    List<String> first = new ArrayList<>();
    List<String> second = new ArrayList<>();
    source.subscribe(first::add);
    source.subscribe(second::add);

    source.publish("{\"boat\":\"B-16\"}");

    assertThat(first).containsExactly("{\"boat\":\"B-16\"}");
    assertThat(second).containsExactly("{\"boat\":\"B-16\"}");
  }
}
