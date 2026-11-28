package pt.ua.tqs.barkito.maintenance.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** BR18: SLA deadlines, counted only within the operating hours (BR6) except for towing. */
class SlaPolicyTest {

  private static final ZoneId ZONE = ZoneId.of("Europe/Lisbon");

  private static Instant at(String localDateTime) {
    return LocalDateTime.parse(localDateTime).atZone(ZONE).toInstant();
  }

  private static Instant dueAt(TicketType type, String openedAt) {
    return MaintenanceRules.DEFAULT
        .slaPolicyFor(type)
        .dueAt(at(openedAt), OperatingHours.DEFAULT, ZONE);
  }

  @ParameterizedTest(name = "{0} opened at {1} is due at {2}")
  @CsvSource({
    "CHARGE, 2026-11-26T10:00, 2026-11-26T12:00",
    "CHARGE, 2026-11-26T07:59:59, 2026-11-26T10:00",
    "CHARGE, 2026-11-26T08:00, 2026-11-26T10:00",
    "CHARGE, 2026-11-26T21:59:59, 2026-11-27T09:59:59",
    "CHARGE, 2026-11-26T22:00, 2026-11-27T10:00",
    "CHARGE, 2026-11-26T20:00, 2026-11-26T22:00",
    "CHARGE, 2026-11-26T21:00, 2026-11-27T09:00",
    "CHARGE, 2026-11-26T23:30, 2026-11-27T10:00",
    "CHARGE, 2026-11-26T06:00, 2026-11-26T10:00",
    "RETURN, 2026-11-26T19:00, 2026-11-27T09:00",
    "REPAIR, 2026-11-26T08:00, 2026-11-27T18:00",
    "REPAIR, 2026-11-26T07:30, 2026-11-27T18:00",
    "REPAIR, 2027-03-27T20:00, 2027-03-29T16:00",
    "REPAIR, 2027-10-30T20:00, 2027-11-01T16:00"
  })
  void theClockRunsOnlyWithinOperatingHours(TicketType type, String openedAt, String due) {
    assertThat(dueAt(type, openedAt)).isEqualTo(at(due));
  }

  @ParameterizedTest(name = "TOW opened at {0} is due at {1}")
  @CsvSource({
    "2026-11-26T17:20, 2026-11-26T19:20",
    "2026-11-26T21:30, 2026-11-26T23:30",
    "2027-03-28T00:30, 2027-03-28T03:30"
  })
  void theTowingClockAlwaysRuns(String openedAt, String due) {
    assertThat(dueAt(TicketType.TOW, openedAt)).isEqualTo(at(due));
  }

  @Test
  void onlyTowingHasToBeOnSiteWithin30Minutes() {
    Instant opened = at("2026-11-26T17:20");

    assertThat(MaintenanceRules.DEFAULT.slaPolicyFor(TicketType.TOW).onSiteDueAt(opened))
        .contains(at("2026-11-26T17:50"));
    assertThat(MaintenanceRules.DEFAULT.slaPolicyFor(TicketType.CHARGE).onSiteDueAt(opened))
        .isEmpty();
  }

  @Test
  void operatingHoursMustOpenBeforeTheyClose() {
    LocalTime ten = LocalTime.of(10, 0);

    assertThatThrownBy(() -> new OperatingHours(ten, ten))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
