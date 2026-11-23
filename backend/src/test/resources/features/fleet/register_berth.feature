@BRK-321
Feature: Register berths at an existing dock
  As an operator, I want to add berths to a dock that already exists,
  so that the dock can hold more boats.

  Scenario: Berths added to an existing dock continue its numbering
    Given the dock "Old Market" is registered with 4 berths
    When the operator adds 2 berths to the dock "Old Market"
    Then the dock "Old Market" has the berths 1, 2, 3, 4, 5 and 6
    And the berths 5 and 6 are in service

  Scenario Outline: A dock cannot have more than 10 berths
    Given the dock "Old Market" is registered with <existing> berths
    When the operator adds <added> berths to the dock "Old Market"
    Then the request <outcome>

    Examples:
      | existing | added | outcome                        |
      | 9        | 1     | is accepted with 10 berths     |
      | 9        | 2     | is refused with the status 422 |

  Scenario: At least one berth must be added
    Given the dock "Old Market" is registered with 4 berths
    When the operator adds 0 berths to the dock "Old Market"
    Then the request is refused with the status 422

  Scenario: Berths can only be added to a dock that exists
    When the operator adds 1 berth to a dock that does not exist
    Then the request is refused with the status 404
