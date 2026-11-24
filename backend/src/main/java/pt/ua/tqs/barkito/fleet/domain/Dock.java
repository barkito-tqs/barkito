package pt.ua.tqs.barkito.fleet.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * A dock where trips start and end (US-04). Its berths are stored apart, in the {@link
 * BerthRepository}; the district is optional.
 */
public record Dock(UUID id, String name, String district, Position position) {

  /** US-04: a dock has at least one berth. */
  public static final int MIN_BERTHS = 1;

  /** US-04: a dock has at most ten berths (agreed for US-04, not yet in the BR table). */
  public static final int MAX_BERTHS = 10;

  public Dock {
    Objects.requireNonNull(id);
    Objects.requireNonNull(name);
    Objects.requireNonNull(position);
    if (name.isBlank()) {
      throw new IllegalArgumentException("A dock needs a name");
    }
  }

  /** A new dock with a random id; the name and district are trimmed, a blank district is none. */
  public static Dock register(String name, String district, Position position) {
    String trimmedDistrict = district == null || district.isBlank() ? null : district.strip();
    return new Dock(UUID.randomUUID(), name.strip(), trimmedDistrict, position);
  }

  /** Whether a dock may have this many berths in total (US-04). */
  public static boolean allowsBerthCount(int berths) {
    return berths >= MIN_BERTHS && berths <= MAX_BERTHS;
  }
}
