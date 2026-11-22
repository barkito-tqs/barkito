package pt.ua.tqs.barkito.telemetry.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import pt.ua.tqs.barkito.fleet.application.BoatService;
import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatStatus;
import pt.ua.tqs.barkito.telemetry.domain.TelemetryReading;
import pt.ua.tqs.barkito.telemetry.domain.TelemetrySource;

@ExtendWith(OutputCaptureExtension.class)
class TelemetryServiceTest {

  private static final String PAYLOAD = "{\"boat\":\"B-16\"}";
  private static final BoatStatus STATUS = new BoatStatus(64, null, null, true, true, List.of());

  private final TelemetrySource source = mock(TelemetrySource.class);
  private final TelemetryReadingParser parser = mock(TelemetryReadingParser.class);
  private final BoatService boats = mock(BoatService.class);
  private final TelemetryService service = new TelemetryService(source, parser, boats);

  @Test
  @SuppressWarnings("unchecked")
  void startSubscribesToTheSource() {
    when(parser.parse(PAYLOAD)).thenReturn(Optional.of(new TelemetryReading("B-16", STATUS)));
    when(boats.recordStatus("B-16", STATUS)).thenReturn(Optional.of(mock(Boat.class)));
    ArgumentCaptor<Consumer<String>> subscriber = ArgumentCaptor.forClass(Consumer.class);

    service.start();
    verify(source).subscribe(subscriber.capture());
    subscriber.getValue().accept(PAYLOAD);

    verify(boats).recordStatus("B-16", STATUS);
  }

  @Test
  void anUnknownBoatIsIgnoredAndLogged(CapturedOutput output) {
    when(parser.parse(PAYLOAD)).thenReturn(Optional.of(new TelemetryReading("B-99", STATUS)));
    when(boats.recordStatus("B-99", STATUS)).thenReturn(Optional.empty());

    service.accept(PAYLOAD);

    assertThat(output).contains("Ignoring telemetry for unknown boat").contains("B-99");
  }

  @Test
  void anInvalidPayloadSavesNothing() {
    when(parser.parse("nonsense")).thenReturn(Optional.empty());

    service.accept("nonsense");

    verify(boats, never()).recordStatus(any(), any());
  }
}
