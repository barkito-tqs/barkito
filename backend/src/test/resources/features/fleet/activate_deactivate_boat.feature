@BRK-44
Feature: Activate and deactivate a boat
  As an operator, I want to activate and deactivate a boat,
  so that I control which boats are in service.

  @BR19 @BR5 @BRK-184
  Scenario Outline: An inactive boat moored and plugged in is activated
    Given the boat "<code>" is in the state "INACTIVE"
    And telemetry reports the boat "<code>" moored and plugged in with <battery> % battery
    When the operator activates the boat "<code>"
    Then the boat "<code>" is now in the state "<state>"

    Examples:
      | code | battery | state     |
      | B-30 | 29      | CHARGING  |
      | B-31 | 30      | AVAILABLE |

  @BR19 @BR5 @BRK-185
  Scenario: An activated boat becomes available once its battery reaches 30 %
    Given the boat "B-32" is in the state "INACTIVE"
    And telemetry reports the boat "B-32" moored and plugged in with 29 % battery
    And the operator activates the boat "B-32"
    When telemetry reports the boat "B-32" with 30 % battery
    Then the boat "B-32" is now in the state "AVAILABLE"

  @BR19 @BRK-186
  Scenario: A boat not plugged in is not activated
    Given the boat "B-33" is in the state "INACTIVE"
    And telemetry reports the boat "B-33" moored but not plugged in
    When the operator activates the boat "B-33"
    Then the operator's request is refused with the status 409
    And the boat "B-33" is now in the state "INACTIVE"

  @BR19 @BRK-187
  Scenario: A boat not moored in a berth is not activated
    Given the boat "B-34" is in the state "INACTIVE"
    And telemetry reports the boat "B-34" plugged in but not moored
    When the operator activates the boat "B-34"
    Then the operator's request is refused with the status 409
    And the boat "B-34" is now in the state "INACTIVE"

  @BR19 @BRK-188
  Scenario: A boat that has not reported telemetry yet is not activated
    Given the boat "B-35" is in the state "INACTIVE"
    When the operator activates the boat "B-35"
    Then the operator's request is refused with the status 409
    And the boat "B-35" is now in the state "INACTIVE"

  @BR19 @BRK-189
  Scenario Outline: An available or charging boat is deactivated
    Given the boat "<code>" is in the state "<state>"
    When the operator deactivates the boat "<code>"
    Then the boat "<code>" is now in the state "INACTIVE"

    Examples:
      | code | state     |
      | B-36 | AVAILABLE |
      | B-37 | CHARGING  |

  @BR19 @BRK-190
  Scenario Outline: A boat on a trip or reserved is not deactivated
    Given the boat "<code>" is in the state "<state>"
    When the operator deactivates the boat "<code>"
    Then the operator's request is refused with the status 409
    And the boat "<code>" is now in the state "<state>"

    Examples:
      | code | state    |
      | B-38 | IN_USE   |
      | B-39 | RESERVED |

  @BRK-191
  Scenario: A deactivated boat stays inactive while telemetry keeps reporting
    Given the boat "B-40" is in the state "AVAILABLE"
    And the operator deactivates the boat "B-40"
    When telemetry reports the boat "B-40" with 80 % battery
    Then the boat "B-40" is now in the state "INACTIVE"

  @BRK-192
  Scenario: A boat that does not exist cannot be activated
    When the operator activates a boat that does not exist
    Then the operator's request is refused with the status 404
