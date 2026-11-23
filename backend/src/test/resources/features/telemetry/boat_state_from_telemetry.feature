@BRK-45
Feature: Boat state updated from telemetry
  As an operator, I want each boat's battery, position, charging state and faults to be updated from telemetry,
  so that the fleet state in Barkito matches the real boats.

  @BRK-171
  Scenario: The latest reading replaces the previous one
    Given the boat "B-70" is in the state "CHARGING"
    When telemetry reports the boat "B-70" with 64 % battery
    And telemetry reports the boat "B-70" with 58 % battery
    Then the operator sees the boat "B-70" with 58 % battery, locked and plugged in

  @BRK-172
  Scenario: A boat moored in a berth reports it
    Given the boat "B-71" is in the state "CHARGING"
    When telemetry sends the reading:
      """
      {"boat":"B-71","battery":50,"berth":"0f8fad5b-d9cb-469f-a165-70867728950e","locked":true,"pluggedIn":true,"faults":[]}
      """
    Then the operator sees the boat "B-71" moored in the berth "0f8fad5b-d9cb-469f-a165-70867728950e"

  @BR5 @BRK-173
  Scenario Outline: A charging boat becomes available when its battery reaches 30 %
    Given the boat "<code>" is in the state "CHARGING"
    When telemetry reports the boat "<code>" with <battery> % battery
    Then the boat "<code>" is now in the state "<state>"

    Examples:
      | code | battery | state     |
      | B-72 | 29      | CHARGING  |
      | B-73 | 30      | AVAILABLE |
      | B-74 | 100     | AVAILABLE |

  @BRK-174
  Scenario Outline: A fault sends an available or charging boat to repair
    Given the boat "<code>" is in the state "<initial>"
    When telemetry reports the boat "<code>" with 80 % battery and the fault "MOTOR_OVERHEAT"
    Then the boat "<code>" is now in the state "REPAIR_REQUIRED"

    Examples:
      | code | initial   |
      | B-75 | AVAILABLE |
      | B-76 | CHARGING  |

  @BRK-175
  Scenario: A fault takes priority over a full battery
    Given the boat "B-77" is in the state "CHARGING"
    When telemetry reports the boat "B-77" with 100 % battery and the fault "MOTOR_OVERHEAT"
    Then the boat "B-77" is now in the state "REPAIR_REQUIRED"

  @BRK-176
  Scenario Outline: Boats in other states keep their state
    Given the boat "<code>" is in the state "<state>"
    When telemetry reports the boat "<code>" with 80 % battery and the fault "MOTOR_OVERHEAT"
    Then the boat "<code>" is now in the state "<state>"

    Examples:
      | code | state           |
      | B-78 | INACTIVE        |
      | B-79 | RESERVED        |
      | B-80 | IN_USE          |
      | B-81 | REPAIR_REQUIRED |

  @BRK-177
  Scenario Outline: An invalid reading leaves the boat untouched
    Given the boat "<code>" is in the state "CHARGING"
    When telemetry sends the reading:
      """
      <reading>
      """
    Then the boat "<code>" is now in the state "CHARGING"
    And the operator sees the boat "<code>" without a status

    Examples:
      | code | reading                                                                      |
      | B-82 | not json                                                                     |
      | B-83 | {"boat":"B-83","battery":101,"locked":true,"pluggedIn":true,"faults":[]}     |
      | B-84 | {"boat":"B-84","locked":true,"pluggedIn":true,"faults":[]}                   |

  @BRK-178
  Scenario: A reading for an unknown boat is ignored
    When telemetry reports the boat "B-99" with 80 % battery
    Then no boat "B-99" appears in the fleet list
