package pt.ua.tqs.barkito.fleet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatRepository;
import pt.ua.tqs.barkito.fleet.domain.BoatState;
import pt.ua.tqs.barkito.fleet.domain.BoatStatus;
import pt.ua.tqs.barkito.fleet.domain.BoatType;
import pt.ua.tqs.barkito.fleet.domain.DockRepository;

@ExtendWith(MockitoExtension.class)
class BoatServiceStatusTest {

  private static final int BATTERY_PERCENT = 80;

  @Mock private BoatRepository boats;

  @Mock private DockRepository docks;

  @Mock private ApplicationEventPublisher events;

  @InjectMocks private BoatService service;

  private static BoatStatus reading() {
    return new BoatStatus(BATTERY_PERCENT, null, null, true, false, List.of());
  }

  private static Boat boat() {
    return Boat.restore(
        UUID.randomUUID(), "B-01", BoatType.STANDARD, UUID.randomUUID(), BoatState.AVAILABLE, null);
  }

  @Test
  void recordsTheStatusAndSavesTheBoat() {
    Boat boat = boat();
    BoatStatus status = reading();
    when(boats.findByCode("B-01")).thenReturn(Optional.of(boat));
    when(boats.save(any(Boat.class))).thenAnswer(call -> call.getArgument(0));

    Optional<Boat> updated = service.recordStatus("B-01", status);

    assertThat(updated).isPresent();
    assertThat(updated.get().status()).contains(status);
    verify(boats).save(updated.get());
  }

  @Test
  void findsTheBoatWhateverTheCaseOrPaddingOfTheCode() {
    when(boats.findByCode("B-01")).thenReturn(Optional.of(boat()));
    when(boats.save(any(Boat.class))).thenAnswer(call -> call.getArgument(0));

    service.recordStatus("  b-01 ", reading());

    verify(boats).findByCode("B-01");
  }

  @Test
  void ignoresAnUnknownBoat() {
    when(boats.findByCode("B-99")).thenReturn(Optional.empty());

    assertThat(service.recordStatus("B-99", reading())).isEmpty();

    verify(boats, never()).save(any());
  }
}
