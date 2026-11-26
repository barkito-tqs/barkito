package pt.ua.tqs.barkito.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The clock domain code reads the time from (QA Manual 3.1), so that time-based rules can be tested
 * with a fixed clock. Its zone is the one the operating hours of Ria Nova are set in (BR6, BR18).
 */
@Configuration
public class ClockConfig {

  /** Ria Nova is a city on the Portuguese coast. */
  static final ZoneId RIA_NOVA_ZONE = ZoneId.of("Europe/Lisbon");

  @Bean
  Clock clock() {
    return Clock.system(RIA_NOVA_ZONE);
  }
}
