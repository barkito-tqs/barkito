package pt.ua.tqs.barkito.telemetry.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import pt.ua.tqs.barkito.fleet.domain.BoatStatus;
import pt.ua.tqs.barkito.fleet.domain.Position;
import pt.ua.tqs.barkito.telemetry.domain.TelemetryReading;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(OutputCaptureExtension.class)
class TelemetryReadingParserTest {

  private static final UUID BERTH = UUID.fromString("0b1f1c1e-6c1a-4a53-9a43-1f0d8a2f7c11");

  private final TelemetryReadingParser parser = new TelemetryReadingParser(new ObjectMapper());

  @Test
  void parsesAFullReading() {
    String payload =
        """
				{"boat":"B-16","battery":64,"berth":"0b1f1c1e-6c1a-4a53-9a43-1f0d8a2f7c11",
				"position":{"latitude":40.64,"longitude":-8.65},"locked":true,"pluggedIn":true,"faults":["motor"]}""";

    Optional<TelemetryReading> reading = parser.parse(payload);

    assertThat(reading)
        .contains(
            new TelemetryReading(
                "B-16",
                new BoatStatus(
                    64, BERTH, new Position(40.64, -8.65), true, true, List.of("motor"))));
  }

  @Test
  void parsesAReadingWithOnlyTheRequiredFields() {
    String payload =
        """
				{"boat":"B-16","battery":0,"locked":false,"pluggedIn":false,"faults":[]}""";

    Optional<TelemetryReading> reading = parser.parse(payload);

    assertThat(reading)
        .contains(
            new TelemetryReading("B-16", new BoatStatus(0, null, null, false, false, List.of())));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "not json",
        "null",
        "[]",
        "{\"boat\":\"B-16\",\"locked\":true,\"pluggedIn\":true,\"faults\":[]}",
        "{\"boat\":\"B-16\",\"battery\":101,\"locked\":true,\"pluggedIn\":true,\"faults\":[]}",
        "{\"boat\":\"B-16\",\"battery\":-1,\"locked\":true,\"pluggedIn\":true,\"faults\":[]}",
        "{\"boat\":\"B-16\",\"battery\":64.5,\"locked\":true,\"pluggedIn\":true,\"faults\":[]}",
        "{\"boat\":\"B-16\",\"battery\":64,\"berth\":\"not-a-uuid\",\"locked\":true,\"pluggedIn\":true,\"faults\":[]}",
        "{\"boat\":\"  \",\"battery\":64,\"locked\":true,\"pluggedIn\":true,\"faults\":[]}",
        "{\"boat\":\"B-16\",\"battery\":64,\"locked\":true,\"pluggedIn\":true,\"faults\":\"none\"}",
        "{\"boat\":\"B-16\",\"battery\":64,\"locked\":\"yes\",\"pluggedIn\":true,\"faults\":[]}"
      })
  void rejectsAnInvalidReadingAndLogsIt(String payload, CapturedOutput output) {
    assertThat(parser.parse(payload)).isEmpty();
    assertThat(output).contains("Rejected telemetry reading");
  }
}
