package pt.ua.tqs.barkito.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.ua.tqs.barkito.maintenance.domain.MaintenanceRules;

/**
 * The single source of the maintenance rule parameters (BR6, BR17, BR18). The rules of use (US-26)
 * and the agreed SLAs (US-28) will replace the defaults here, and nowhere else.
 */
@Configuration
public class MaintenanceRulesConfig {

  @Bean
  MaintenanceRules maintenanceRules() {
    return MaintenanceRules.DEFAULT;
  }
}
