@context @scenario
Feature: Capsule pedagogical scenario injection (CAP-SPD-4)

  As a capsule-gradle producer
  I want to inject the pedagogical scenario (objectives, duration, prerequisites) into the augmented context
  So that content generation anchors speaker notes in the session objectives instead of generic prompts

  Scenario: The scenarioFile CLI param resolves a scenario directory and feeds the scenario section
    Given a Capsule scenario context Gradle project with the capsule plugin applied
    And EAGER governance files are present for scenario context
    And a pedagogical scenario directory is present with metadata and adoc
    When I collect the augmented context with scenario file pointing to the scenario directory
    Then the scenario context build succeeds
    And the scenario context artefact contains "Pedagogical Scenario"
    And the scenario context artefact contains "Comprendre le cadre de la formation"

  Scenario: No scenarioFile configured produces no scenario section
    Given a Capsule scenario context Gradle project with the capsule plugin applied
    And EAGER governance files are present for scenario context
    When I collect the augmented context with no scenario configuration
    Then the scenario context build succeeds
    And the scenario context artefact omits "Pedagogical Scenario"

  Scenario: A malformed metadata json is gracefully skipped and the adoc is still parsed
    Given a Capsule scenario context Gradle project with the capsule plugin applied
    And EAGER governance files are present for scenario context
    And a pedagogical scenario directory is present with malformed metadata and valid adoc
    When I collect the augmented context with scenario file pointing to the scenario directory
    Then the scenario context build succeeds
    And the scenario context artefact contains "Pedagogical Scenario"
    And the scenario context artefact contains "Goal from valid adoc"