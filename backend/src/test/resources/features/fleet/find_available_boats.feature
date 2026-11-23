@BRK-46
Feature: Find available boats by dock and type
  As an end user, I want to see the boats available at each dock, by type,
  so that I know where to go before I walk to a dock.

  Background:
    Given the docks "Old Market" and "Fish Quay" exist

  @BR5 @BRK-179
  Scenario Outline: A boat is listed only when it is available with at least 30 % battery
    Given the boat "B-01" of type "STANDARD" is <state> at the dock "Old Market" with <battery> % battery
    When the user looks for boats at the dock "Old Market"
    Then the boat "B-01" is <listed>

    Examples:
      | state     | battery | listed     |
      | AVAILABLE | 30      | listed     |
      | AVAILABLE | 29      | not listed |
      | AVAILABLE | 100     | listed     |
      | CHARGING  | 80      | not listed |
      | RESERVED  | 80      | not listed |

  @BRK-180
  Scenario: The list is filtered by dock and by type
    Given the boat "B-01" of type "STANDARD" is AVAILABLE at the dock "Old Market" with 80 % battery
    And the boat "B-02" of type "XL" is AVAILABLE at the dock "Old Market" with 80 % battery
    And the boat "B-03" of type "XL" is AVAILABLE at the dock "Fish Quay" with 80 % battery
    When the user looks for boats of type "XL" at the dock "Old Market"
    Then only the boat "B-02" is listed

  @BRK-181
  Scenario: Each boat shows its type and battery level
    Given the boat "B-01" of type "XL" is AVAILABLE at the dock "Old Market" with 64 % battery
    When the user looks for boats at the dock "Old Market"
    Then the boat "B-01" is shown with the type "XL" and 64 % battery

  @BRK-182
  Scenario: A dock with no available boats shows none
    Given the boat "B-01" of type "STANDARD" is CHARGING at the dock "Fish Quay" with 20 % battery
    When the user looks for boats at the dock "Fish Quay"
    Then no boats are listed
