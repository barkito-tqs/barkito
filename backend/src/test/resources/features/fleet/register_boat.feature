@BRK-42
Feature: Register a boat
  As an operator, I want to register a boat with its code, type and home dock,
  so that it joins the fleet.

  Background:
    Given the dock "Old Market" exists

  Scenario Outline: A new boat of an accepted type starts inactive and joins the fleet list
    When the operator registers the boat "<code>" of type "<type>" at the dock "Old Market"
    Then the boat is registered with the state "INACTIVE"
    And the boat "<code>" appears in the operator's fleet list

    Examples:
      | code | type     |
      | B-16 | STANDARD |
      | B-17 | XL       |

  Scenario: A type other than Standard or XL is refused
    When the operator registers the boat "B-18" of type "KAYAK" at the dock "Old Market"
    Then the registration is refused with the status 400

  Scenario: A duplicate code is refused with 409
    Given the boat "B-19" is registered at the dock "Old Market"
    When the operator registers the boat "B-19" of type "XL" at the dock "Old Market"
    Then the registration is refused with the status 409

  Scenario: The home dock must exist
    When the operator registers the boat "B-20" of type "STANDARD" at a dock that does not exist
    Then the registration is refused with the status 404
