@ci
Feature: Cucumber test skip guard

  As a capsule-gradle maintainer
  I want the cucumberTest task to be skipped unless explicitly requested
  So that local builds stay fast and CI remains the only place running the full BDD suite

  Scenario: cucumberTest is skipped when no flag is active
    Given a cucumber test guard with runCucumber "inactive" and CI "inactive"
    Then the guard should skip the cucumber test
    And the skip reason should mention "-PrunCucumber"
    And the skip reason should mention "CI"

  Scenario: cucumberTest runs when runCucumber property is active
    Given a cucumber test guard with runCucumber "active" and CI "inactive"
    Then the guard should run the cucumber test
    And the skip reason should be blank

  Scenario: cucumberTest runs when CI env var is true
    Given a cucumber test guard with runCucumber "inactive" and CI "active"
    Then the guard should run the cucumber test
    And the skip reason should be blank

  Scenario: cucumberTest runs when both runCucumber and CI are active
    Given a cucumber test guard with runCucumber "active" and CI "active"
    Then the guard should run the cucumber test
    And the skip reason should be blank