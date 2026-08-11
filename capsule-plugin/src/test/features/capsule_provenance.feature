@context @provenance
Feature: Capsule augmented context provenance (CAP-PROVENANCE US-2)

  As a capsule-gradle producer
  I want the augmented context collection to emit a context-provenance.json audit artefact
  So that content generation can be audited against the exact sources that fed the prompt

  Scenario: The provenance JSON is generated with EAGER RAG and Graphify channels tracked
    Given a Capsule provenance Gradle project with the capsule plugin applied
    And EAGER governance files are present for provenance
    When I collect the augmented context with RAG and Graphify content for provenance
    Then the provenance build succeeds
    And the provenance artefact contains 3 channels
    And the provenance EAGER channel has 3 sources
    And the provenance artefact contains a "rag-injected" source
    And the provenance artefact contains a "graphify-injected" source
    And the provenance log line mentions the provenance artefact

  Scenario: The provenance channels carry file names chars and tokens
    Given a Capsule provenance Gradle project with the capsule plugin applied
    And EAGER governance files are present for provenance
    When I collect the augmented context with RAG content for provenance
    Then the provenance EAGER sources are named adoc governance files
    And the provenance EAGER sources have positive chars and tokens
    And the provenance total tokens is positive

  Scenario: An empty context yields an empty provenance JSON
    Given a Capsule provenance Gradle project with the capsule plugin applied
    When I collect the augmented context with no content for provenance
    Then the provenance build succeeds
    And the provenance JSON has empty channels
    And the provenance JSON has zero total chars and tokens
