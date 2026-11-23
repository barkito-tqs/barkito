package pt.ua.tqs.barkito.fleet.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatRepository;
import pt.ua.tqs.barkito.fleet.domain.BoatState;
import pt.ua.tqs.barkito.fleet.domain.BoatType;
import pt.ua.tqs.barkito.fleet.domain.DockRepository;

@ExtendWith(MockitoExtension.class)
class BoatServiceTest {

  private static final UUID DOCK = UUID.randomUUID();

  @Mock private BoatRepository boats;

  @Mock private DockRepository docks;

  @InjectMocks private BoatService service;

  @BeforeEach
  void saveReturnsTheBoat() {
    when(boats.save(any(Boat.class))).thenAnswer(call -> call.getArgument(0));
  }

  @Test
  void registersANewBoatAsInactive() {
    when(docks.existsById(DOCK)).thenReturn(true);

    Boat boat = service.register("B-16", BoatType.STANDARD, DOCK);

    assertThat(boat.state()).isEqualTo(BoatState.INACTIVE);
    assertThat(boat.homeDockId()).isEqualTo(DOCK);
    verify(boats).save(boat);
  }

  @Test
  void normalisesTheCodeSoDuplicatesDifferingInCaseAreCaught() {
    when(docks.existsById(DOCK)).thenReturn(true);

    Boat boat = service.register("  b-16 ", BoatType.XL, DOCK);

    assertThat(boat.code()).isEqualTo("B-16");
    verify(boats).existsByCode("B-16");
  }
}
