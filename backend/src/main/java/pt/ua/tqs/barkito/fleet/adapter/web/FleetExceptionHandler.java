package pt.ua.tqs.barkito.fleet.adapter.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pt.ua.tqs.barkito.fleet.application.BoatNotFoundException;
import pt.ua.tqs.barkito.fleet.application.DockNotFoundException;
import pt.ua.tqs.barkito.fleet.application.DuplicateBoatCodeException;
import pt.ua.tqs.barkito.fleet.domain.BoatTransitionRefusedException;

/** Maps fleet errors to RFC 9457 problem details. */
@RestControllerAdvice(assignableTypes = {BoatController.class, AvailableBoatController.class})
public class FleetExceptionHandler {

  @ExceptionHandler(DuplicateBoatCodeException.class)
  ProblemDetail duplicateCode(DuplicateBoatCodeException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
  }

  @ExceptionHandler(DockNotFoundException.class)
  ProblemDetail dockNotFound(DockNotFoundException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(BoatNotFoundException.class)
  ProblemDetail boatNotFound(BoatNotFoundException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(BoatTransitionRefusedException.class)
  ProblemDetail transitionRefused(BoatTransitionRefusedException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
  }
}
