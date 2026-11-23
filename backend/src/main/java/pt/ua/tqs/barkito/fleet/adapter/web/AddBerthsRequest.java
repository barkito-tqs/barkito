package pt.ua.tqs.barkito.fleet.adapter.web;

import jakarta.validation.constraints.NotNull;

/** BRK-321: how many berths to add to a dock; the total is checked by the service (422). */
public record AddBerthsRequest(@NotNull Integer count) {}
