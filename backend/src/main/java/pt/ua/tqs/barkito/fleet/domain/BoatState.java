package pt.ua.tqs.barkito.fleet.domain;

/** Boat lifecycle states (Product Specification 3.3). */
public enum BoatState {
  INACTIVE,
  CHARGING,
  AVAILABLE,
  RESERVED,
  IN_USE,
  CHARGE_REQUIRED,
  REPAIR_REQUIRED,
  TOW_REQUIRED,
  RETURN_REQUIRED,
  RETIRED
}
