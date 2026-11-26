@BRK-58
Feature: Tickets opened and assigned automatically
  As an operator, I want a ticket to be opened and assigned automatically whenever a boat needs a technician,
  so that every problem has an owner and a deadline.

  The docks of these scenarios are far from the docks seeded by the dev profile, so the
  nearest covered dock is always one of them.

  Background:
    Given the dock "Rossio" is at 41.1500, -8.6100
    And the dock "Ponte" is at 41.1480, -8.6000
    And the dock "Barra" is at 41.1600, -8.6800
    And the Field Technician "Inês Rocha" covers the dock "Rossio"
    And the Field Technician "Duarte Lopes" covers the dock "Barra"

  @BR17 @BR18 @BRK-418
  Scenario: A boat with a fault gets a repair ticket from the technician of its dock
    Given the boat "B-50" is AVAILABLE in a berth of the dock "Rossio"
    When telemetry reports a fault on the boat "B-50"
    Then the boat "B-50" is now in the state "REPAIR_REQUIRED"
    And the operator sees an open REPAIR ticket for the boat "B-50" assigned to "Inês Rocha"
    And the ticket of the boat "B-50" has an SLA deadline after it was opened

  @BR17 @BRK-419
  Scenario: A dock without a technician gets the technician of the nearest covered dock
    Given the boat "B-51" is CHARGING in a berth of the dock "Ponte"
    When telemetry reports a fault on the boat "B-51"
    Then the operator sees an open REPAIR ticket for the boat "B-51" assigned to "Inês Rocha"

  @BRK-420
  Scenario: A boat that does not need a technician gets no ticket
    Given the boat "B-52" is CHARGING in a berth of the dock "Rossio"
    When telemetry reports the boat "B-52" charged to 80 %
    Then the boat "B-52" is now in the state "AVAILABLE"
    And the boat "B-52" has no ticket

  @BRK-421
  Scenario: A boat already waiting for a repair gets no second ticket
    Given the boat "B-53" is AVAILABLE in a berth of the dock "Rossio"
    And telemetry reports a fault on the boat "B-53"
    When telemetry reports a fault on the boat "B-53"
    Then the boat "B-53" has 1 ticket
