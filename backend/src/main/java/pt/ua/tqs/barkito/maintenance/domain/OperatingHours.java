package pt.ua.tqs.barkito.maintenance.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * The daily operating hours (BR6), during which the SLA clock of most tickets runs (BR18). The
 * client will set them in the rules of use (US-26); until then the defaults apply.
 */
public record OperatingHours(LocalTime opensAt, LocalTime closesAt) {

  /** BR6: boats operate from 08:00 to 22:00 by default. */
  public static final OperatingHours DEFAULT =
      new OperatingHours(LocalTime.of(8, 0), LocalTime.of(22, 0));

  public OperatingHours {
    Objects.requireNonNull(opensAt);
    Objects.requireNonNull(closesAt);
    if (!opensAt.isBefore(closesAt)) {
      throw new IllegalArgumentException("Operating hours must open before they close");
    }
  }

  /**
   * The instant reached by counting {@code duration} from {@code start}, counting only the time
   * within the operating hours of each day in the given zone.
   */
  public Instant addOperatingTime(Instant start, Duration duration, ZoneId zone) {
    ZonedDateTime cursor = start.atZone(zone);
    Duration remaining = duration;
    while (true) {
      ZonedDateTime opening = cursor.with(opensAt);
      ZonedDateTime closing = cursor.with(closesAt);
      if (cursor.isBefore(opening)) {
        cursor = opening;
      } else if (!cursor.isBefore(closing)) {
        cursor = opening.plusDays(1);
        continue;
      }
      Duration leftToday = Duration.between(cursor, closing);
      if (remaining.compareTo(leftToday) <= 0) {
        return cursor.plus(remaining).toInstant();
      }
      remaining = remaining.minus(leftToday);
      cursor = opening.plusDays(1);
    }
  }
}
