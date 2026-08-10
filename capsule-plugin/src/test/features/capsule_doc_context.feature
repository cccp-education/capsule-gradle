@context @docs
Feature: Capsule documentary corpus injection (CAP-DOCCONTEXT)

  As a capsule-gradle producer
  I want to inject the documentary corpus (AFNOR/REAC manuals) into the Docs channel
  So that content generation feeds on the real training material instead of an empty prompt
  assembled from Ant-style globs resolved by the Gradle wiring layer

  Scenario: The docsContent CLI param still works when no globs are configured (retrocompat)
    Given a Capsule doc context Gradle project with the capsule plugin applied
    And EAGER governance files are present for doc context
    When I collect the augmented context with doc content "AFNOR grading manual via CLI"
    Then the doc context build succeeds
    And the doc context artefact contains "CONTEXTE_DOCS"
    And the doc context artefact contains "AFNOR grading manual via CLI"

  Scenario: The docsGlobs CLI param resolves files and feeds the Docs channel
    Given a Capsule doc context Gradle project with the capsule plugin applied
    And EAGER governance files are present for doc context
    And documentary corpus files are present under "docs/afnor"
    When I collect the augmented context with doc globs "docs/afnor/**/*.adoc"
    Then the doc context build succeeds
    And the doc context artefact contains "CONTEXTE_DOCS"
    And the doc context artefact contains "AFNOR grading rules for FPA"
    And the doc context artefact contains "Evaluation criteria for competency"

  Scenario: The docsGlobs CLI param overrides the docsContent string
    Given a Capsule doc context Gradle project with the capsule plugin applied
    And EAGER governance files are present for doc context
    And documentary corpus files are present under "docs/afnor"
    When I collect the augmented context with doc content "legacy string" and doc globs "docs/afnor/**/*.adoc"
    Then the doc context build succeeds
    And the doc context artefact contains "AFNOR grading rules for FPA"
    And the doc context artefact omits "legacy string"

  Scenario: Empty globs and empty docsContent produce no Docs channel
    Given a Capsule doc context Gradle project with the capsule plugin applied
    And EAGER governance files are present for doc context
    When I collect the augmented context with no doc configuration
    Then the doc context build succeeds
    And the doc context artefact omits "CONTEXTE_DOCS"