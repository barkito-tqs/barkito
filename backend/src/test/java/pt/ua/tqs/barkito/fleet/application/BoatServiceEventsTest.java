package pt.ua.tqs.barkito.fleet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatRepository;
import pt.ua.tqs.barkito.fleet.domain.BoatState;
import pt.ua.tqs.barkito.fleet.domain.BoatStateChanged;
import pt.ua.tqs.barkito.fleet.domain.BoatStatus;
import pt.ua.tqs.barkito.fleet.domain.BoatType;
import pt.ua.tqs.barkito.fleet.domain.DockRepository;

/** Every change of a boat's state is published for the other modules (Product Spec 4.3). */
@ExtendWith(MockitoExtension.class)
class BoatServiceEventsTest {

  private static final int BATTERY_PERCENT = 80;

  @Mock private BoatRepository boats;

  @Mock private DockRepository docks;

  @Mock private ApplicationEventPublisher events;

  @InjectMocks private BoatService service;

  private static BoatStatus reading(List<String> faults) {
    return new BoatStatus(BATTERY_PERCENT, UUID.randomUUID(), null, true, true, faults);
  }

  private static Boat boatIn(BoatState state) {
    return Boat.restore(
        UUID.randomUUID(), "B-07", BoatType.STANDARD, UUID.randomUUID(), state, reading(List.of()));
  }

  @BeforeEach
  void saveReturnsTheBoat() {
    when(boats.save(any(Boat.class))).thenAnswer(call -> call.getArgument(0));
  }

  @Test
  void aFaultReportedByTelemetryPublishesTheNewState() {
    when(boats.findByCode("B-07")).thenReturn(Optional.of(boatIn(BoatState.AVAILABLE)));

    service.recordStatus("B-07", reading(List.of("MOTOR_OVERHEAT")));

    ArgumentCaptor<BoatStateChanged> event = ArgumentCaptor.forClass(BoatStateChanged.class);
    verify(events).publishEvent(event.capture());
    assertThat(event.getValue().boat().state()).isEqualTo(BoatState.REPAIR_REQUIRED);
    assertThat(event.getValue().previousState()).isEqualTo(BoatState.AVAILABLE);
  }

  @Test
  void aReadingThatKeepsTheStatePublishesNothing() {
    when(boats.findByCode("B-07")).thenReturn(Optional.of(boatIn(BoatState.AVAILABLE)));

    service.recordStatus("B-07", reading(List.of()));

    verify(events, never()).publishEvent(any());
  }

  @Test
  void activatingAndDeactivatingPublishTheirChanges() {
    Boat inactive = boatIn(BoatState.INACTIVE);
    when(boats.findById(inactive.id())).thenReturn(Optional.of(inactive));
    Boat available = service.activate(inactive.id());
    when(boats.findById(inactive.id())).thenReturn(Optional.of(available));

    service.deactivate(inactive.id());

    ArgumentCaptor<BoatStateChanged> event = ArgumentCaptor.forClass(BoatStateChanged.class);
    verify(events, times(2)).publishEvent(event.capture());
    assertThat(event.getAllValues())
        .extracting(BoatStateChanged::previousState)
        .containsExactly(BoatState.INACTIVE, BoatState.AVAILABLE);
  }
}
