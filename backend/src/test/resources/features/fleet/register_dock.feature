@BRK-43
Feature: Register docks and berths
  As an operator, I want to register a dock with its location and berths,
  so that boats have places where trips start and end.

  Scenario: A new dock has its berths numbered from 1 and in service
    When the operator registers the dock "Fish Market" at 40.6405, -8.6538 with 4 berths
    Then the dock "Fish Market" has the berths 1, 2, 3 and 4
    And the berths 1, 2, 3 and 4 are in service
    And the dock "Fish Market" is located at 40.6405, -8.6538
    And the dock "Fish Market" appears in the list of docks

  Scenario Outline: A dock has from 1 to 10 berths
    When the operator registers the dock "Fish Market" with <berths> berths
    Then the dock request <outcome>

    Examples:
      | berths | outcome                        |
      | 0      | is refused with the status 422 |
      | 1      | is accepted with 1 berths      |
      | 10     | is accepted with 10 berths     |
      | 11     | is refused with the status 422 |

  Scenario: A dock name already taken is refused, ignoring case and surrounding spaces
    Given the dock "Fish Market" is registered with 2 berths
    When the operator registers the dock "  fish MARKET " with 3 berths
    Then the dock request is refused with the status 409

  Scenario Outline: A dock needs a valid location
    When the operator registers the dock "Fish Market" at <latitude>, <longitude> with 4 berths
    Then the dock request is refused with the status 400

    Examples:
      | latitude | longitude |
      | 90.0001  | -8.6538   |
      | 40.6405  | -180.0001 |

  Scenario: A dock without a location is refused
    When the operator registers the dock "Fish Market" without a location
    Then the dock request is refused with the status 400
