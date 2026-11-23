package pt.ua.tqs.barkito.fleet.adapter.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * US-04: a new dock. The number of berths is checked by the service (1 to 10, refused with 422),
 * since it is a business rule.
 */
public record RegisterDockRequest(
    @NotBlank @Size(max = RegisterDockRequest.MAX_NAME_LENGTH) String name,
    @Size(max = RegisterDockRequest.MAX_DISTRICT_LENGTH) String district,
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
    @NotNull Integer berthCount) {

  static final int MAX_NAME_LENGTH = 64;
  static final int MAX_DISTRICT_LENGTH = 64;
}
