package pt.ua.tqs.barkito.fleet.application;

public class DuplicateDockNameException extends RuntimeException {

  public DuplicateDockNameException(String name) {
    super("A dock named " + name + " is already registered.");
  }
}
