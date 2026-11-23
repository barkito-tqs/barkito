package pt.ua.tqs.barkito.fleet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatRepository;
import pt.ua.tqs.barkito.fleet.domain.BoatState;
import pt.ua.tqs.barkito.fleet.domain.BoatStatus;
import pt.ua.tqs.barkito.fleet.domain.BoatTransitionRefusedException;
import pt.ua.tqs.barkito.fleet.domain.BoatType;
import pt.ua.tqs.barkito.fleet.domain.DockRepository;

@ExtendWith(MockitoExtension.class)
class BoatServiceActivationTest {

  private static final int BATTERY_PERCENT = 80;

  @Mock private BoatRepository boats;

  @Mock private DockRepository docks;

  @InjectMocks private BoatService service;

  private static Boat boatIn(BoatState state, boolean pluggedIn) {
    BoatStatus status =
        new BoatStatus(BATTERY_PERCENT, UUID.randomUUID(), null, true, pluggedIn, List.of());
    return Boat.restore(
        UUID.randomUUID(), "B-16", BoatType.STANDARD, UUID.randomUUID(), state, status);
  }

  @Test
  void activatesTheBoatAndSavesIt() {
    Boat boat = boatIn(BoatState.INACTIVE, true);
    when(boats.findById(boat.id())).thenReturn(Optional.of(boat));
    when(boats.save(any(Boat.class))).thenAnswer(call -> call.getArgument(0));

    Boat activated = service.activate(boat.id());

    assertThat(activated.state()).isEqualTo(BoatState.AVAILABLE);
    verify(boats).save(activated);
  }

  @Test
  void deactivatesTheBoatAndSavesIt() {
    Boat boat = boatIn(BoatState.AVAILABLE, true);
    when(boats.findById(boat.id())).thenReturn(Optional.of(boat));
    when(boats.save(any(Boat.class))).thenAnswer(call -> call.getArgument(0));

    Boat deactivated = service.deactivate(boat.id());

    assertThat(deactivated.state()).isEqualTo(BoatState.INACTIVE);
    verify(boats).save(deactivated);
  }

  @Test
  void aRefusedActivationSavesNothing() {
    Boat boat = boatIn(BoatState.INACTIVE, false);
    when(boats.findById(boat.id())).thenReturn(Optional.of(boat));

    assertThatThrownBy(() -> service.activate(boat.id()))
        .isInstanceOf(BoatTransitionRefusedException.class);
    verify(boats, never()).save(any());
  }

  @Test
  void aRefusedDeactivationSavesNothing() {
    Boat boat = boatIn(BoatState.IN_USE, true);
    when(boats.findById(boat.id())).thenReturn(Optional.of(boat));

    assertThatThrownBy(() -> service.deactivate(boat.id()))
        .isInstanceOf(BoatTransitionRefusedException.class);
    verify(boats, never()).save(any());
  }

  @Test
  void anUnknownBoatIsNotFound() {
    UUID id = UUID.randomUUID();
    when(boats.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.activate(id))
        .isInstanceOf(BoatNotFoundException.class)
        .hasMessageContaining(id.toString());
    assertThatThrownBy(() -> service.deactivate(id)).isInstanceOf(BoatNotFoundException.class);
  }
}
