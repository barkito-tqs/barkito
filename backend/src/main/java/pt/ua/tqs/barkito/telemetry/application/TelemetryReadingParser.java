package pt.ua.tqs.barkito.telemetry.application;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pt.ua.tqs.barkito.fleet.domain.BoatStatus;
import pt.ua.tqs.barkito.fleet.domain.Position;
import pt.ua.tqs.barkito.telemetry.domain.TelemetryReading;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Turns a raw JSON payload into a {@link TelemetryReading}, rejecting it when it breaks the JSON
 * Schema.
 */
@Component
public class TelemetryReadingParser {

  private static final Logger LOG = LoggerFactory.getLogger(TelemetryReadingParser.class);
  private static final String SCHEMA_LOCATION = "classpath:schemas/telemetry-reading.schema.json";

  private final ObjectMapper objectMapper;
  private final Schema schema;

  public TelemetryReadingParser(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.schema =
        SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
            .getSchema(SchemaLocation.of(SCHEMA_LOCATION));
    this.schema.initializeValidators();
  }

  /**
   * The reading, or empty (after logging a warning) when the payload is not a valid reading. Never
   * throws.
   */
  public Optional<TelemetryReading> parse(String payload) {
    try {
      List<Error> errors =
          schema.validate(
              payload,
              InputFormat.JSON,
              context -> context.executionConfig(config -> config.formatAssertionsEnabled(true)));
      if (!errors.isEmpty()) {
        LOG.warn("Rejected telemetry reading: {}", errors);
        return Optional.empty();
      }
      return Optional.of(toReading(objectMapper.readTree(payload)));
    } catch (RuntimeException e) {
      LOG.warn("Rejected telemetry reading: {}", e.getMessage());
      return Optional.empty();
    }
  }

  private TelemetryReading toReading(JsonNode json) {
    JsonNode berth = json.get("berth");
    JsonNode position = json.get("position");
    List<String> faults =
        json.get("faults").values().stream().map(node -> node.asString()).toList();
    BoatStatus status =
        new BoatStatus(
            json.get("battery").asInt(),
            berth == null ? null : UUID.fromString(berth.asString()),
            position == null
                ? null
                : new Position(
                    position.get("latitude").asDouble(), position.get("longitude").asDouble()),
            json.get("locked").asBoolean(),
            json.get("pluggedIn").asBoolean(),
            faults);
    return new TelemetryReading(json.get("boat").asString(), status);
  }
}
