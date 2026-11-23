package pt.ua.tqs.barkito.fleet.adapter.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import pt.ua.tqs.barkito.fleet.domain.BoatType;

public record RegisterBoatRequest(
    @NotBlank @Size(max = RegisterBoatRequest.MAX_CODE_LENGTH) String code,
    @NotNull BoatType type,
    @NotNull UUID homeDockId) {

  /** Boat codes are short hull markings such as "B-16". */
  static final int MAX_CODE_LENGTH = 16;
}
