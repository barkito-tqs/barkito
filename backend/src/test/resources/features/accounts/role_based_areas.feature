@BRK-41
Feature: Role-based areas
  As a user with a role, I want to see only the area of my role,
  so that each actor can do only what they are allowed to.

  Scenario: An operator reaches the fleet list
    Given I am signed in with the role "OPERATOR"
    When I open the fleet list
    Then the request is let through

  Scenario Outline: The fleet list is refused to the other roles
    Given I am signed in with the role "<role>"
    When I open the fleet list
    Then the request is refused with the status 403

    Examples:
      | role           |
      | END_USER       |
      | TECHNICIAN     |
      | CLIENT_MANAGER |
      | ADMIN          |

  Scenario: An end user looks for boats at a dock
    Given I am signed in with the role "END_USER"
    When I look for boats at a dock
    Then the request is let through

  Scenario: The operator cannot use the end user's boat search
    Given I am signed in with the role "OPERATOR"
    When I look for boats at a dock
    Then the request is refused with the status 403

  Scenario: A request without signing in is refused
    Given I am not signed in
    When I open the fleet list
    Then the request is refused with the status 401
