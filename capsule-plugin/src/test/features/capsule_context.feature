@context @rag
Feature: Capsule augmented context collection (CAP-ARCH-2)

  As a capsule-gradle producer
  I want to collect and render the augmented context (EAGER + RAG + Graphify + Docs)
  So that content generation feeds on a budgeted, prompt-ready context
  assembled from the N0 codebase-contracts contract

  Scenario: The augmented context collection task is registered
    Given a Capsule context Gradle project with the capsule plugin applied
    When I list the Gradle tasks for the augmented context
    Then the augmented context task is listed in the output

  Scenario: The task assembles EAGER governance and injected RAG content
    Given a Capsule context Gradle project with the capsule plugin applied
    And EAGER governance files are present
    When I collect the augmented context with RAG content "RAG nugget about grading"
    Then the augmented context build succeeds
    And the augmented context build logs "CAPSULE CONTEXT"
    And the augmented context artefact contains "RÈGLES_EAGER"
    And the augmented context artefact contains "RAG nugget about grading"

  Scenario: The task is skipped when inputs are unchanged
    Given a Capsule context Gradle project with the capsule plugin applied
    And EAGER governance files are present
    When I collect the augmented context with RAG content "unchanged"
    And I collect the augmented context again with RAG content "unchanged"
    Then the augmented context second run is up-to-date

  Scenario: The token budget truncates a large RAG channel
    Given a Capsule context Gradle project with the capsule plugin applied
    And EAGER governance files are present
    When I collect the augmented context with a token budget of 40 and a large RAG block
    Then the augmented context build succeeds
    And the augmented context artefact omits the RAG tail "rag line number 50 with filler words"
