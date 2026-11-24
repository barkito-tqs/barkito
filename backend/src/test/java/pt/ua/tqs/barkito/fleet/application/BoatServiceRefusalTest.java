package pt.ua.tqs.barkito.fleet.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.ua.tqs.barkito.fleet.domain.Boat;
import pt.ua.tqs.barkito.fleet.domain.BoatRepository;
import pt.ua.tqs.barkito.fleet.domain.BoatType;
import pt.ua.tqs.barkito.fleet.domain.DockRepository;

@ExtendWith(MockitoExtension.class)
class BoatServiceRefusalTest {

  private static final UUID DOCK = UUID.randomUUID();

  @Mock private BoatRepository boats;

  @Mock private DockRepository docks;

  @InjectMocks private BoatService service;

  @Test
  void refusesADuplicateCode() {
    when(boats.existsByCode("B-16")).thenReturn(true);

    assertThatThrownBy(() -> service.register("B-16", BoatType.STANDARD, DOCK))
        .isInstanceOf(DuplicateBoatCodeException.class)
        .hasMessageContaining("B-16");
    verify(boats, never()).save(any(Boat.class));
  }

  @Test
  void refusesAHomeDockThatDoesNotExist() {
    when(docks.existsById(DOCK)).thenReturn(false);

    assertThatThrownBy(() -> service.register("B-16", BoatType.STANDARD, DOCK))
        .isInstanceOf(DockNotFoundException.class);
    verify(boats, never()).save(any(Boat.class));
  }
}
