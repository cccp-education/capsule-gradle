@context @glossary
Feature: Capsule glossary injection (CAP-GLOSSARY-3)

  As a capsule-gradle producer
  I want to inject the training referential glossary (official terminology) into the augmented context
  So that content generation aligns speaker notes on AFNOR/REAC terminology instead of generic synonyms

  Scenario: The glossaryFile CLI param resolves a glossary adoc and feeds the glossary section
    Given a Capsule glossary context Gradle project with the capsule plugin applied
    And EAGER governance files are present for glossary context
    And a glossary adoc file is present with valid terms
    When I collect the augmented context with glossary file pointing to the glossary adoc
    Then the glossary context build succeeds
    And the glossary context artefact contains "Official Glossary"
    And the glossary context artefact contains "competence transversale"
    And the glossary context artefact omits "unrelated"

  Scenario: No glossaryFile configured produces no glossary section
    Given a Capsule glossary context Gradle project with the capsule plugin applied
    And EAGER governance files are present for glossary context
    When I collect the augmented context with no glossary configuration
    Then the glossary context build succeeds
    And the glossary context artefact omits "Official Glossary"

  Scenario: A malformed glossary adoc with no Glossary section is gracefully skipped
    Given a Capsule glossary context Gradle project with the capsule plugin applied
    And EAGER governance files are present for glossary context
    And a glossary adoc file is present with no Glossary section
    When I collect the augmented context with glossary file pointing to the glossary adoc
    Then the glossary context build succeeds
    And the glossary context artefact omits "Official Glossary"