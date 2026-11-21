package pt.ua.tqs.barkito.fleet.application;

public class DuplicateBoatCodeException extends RuntimeException {

	public DuplicateBoatCodeException(String code) {
		super("A boat with code " + code + " is already registered.");
	}
}
