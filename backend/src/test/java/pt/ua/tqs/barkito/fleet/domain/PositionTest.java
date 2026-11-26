package pt.ua.tqs.barkito.fleet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class PositionTest {

  private static final Position OLD_MARKET = new Position(40.641817, -8.66465);
  private static final Position WATERFRONT = new Position(40.636023, -8.6426);

  /** One degree of latitude is about 111.2 km on the mean Earth sphere. */
  private static final double ONE_DEGREE_OF_LATITUDE_METRES = 111_195;

  @Test
  void aPositionIsAtNoDistanceFromItself() {
    assertThat(OLD_MARKET.distanceInMetresTo(OLD_MARKET)).isZero();
  }

  @Test
  void oneDegreeOfLatitudeIsAbout111Kilometres() {
    Position north = new Position(41.641817, -8.66465);

    assertThat(OLD_MARKET.distanceInMetresTo(north))
        .isCloseTo(ONE_DEGREE_OF_LATITUDE_METRES, within(10.0));
  }

  @Test
  void theDistanceIsTheSameBothWays() {
    double there = OLD_MARKET.distanceInMetresTo(WATERFRONT);

    assertThat(there).isCloseTo(WATERFRONT.distanceInMetresTo(OLD_MARKET), within(1e-6));
    assertThat(there).isBetween(1_900.0, 2_000.0);
  }
}
