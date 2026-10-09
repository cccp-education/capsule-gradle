@storyboard-context
Feature: Storyboard context (QQOQCP, visual identity, narrative thread)

  As a capsule-gradle producer
  I want the viral storyboard to carry a reviewable context
  So that a capsule is a coherent narrative, not a list of isolated shots

  Scenario: A fully contextualised storyboard round-trips through the document
    Given a fully contextual viral storyboard
    When the contextual storyboard is rendered then parsed back
    Then the parsed contextual storyboard equals the original
    And the contextual storyboard document contains "== QQOQCP"
    And the contextual storyboard document contains "== Identité visuelle (design system)"
    And the contextual storyboard document contains "== Fil conducteur narratif"

  Scenario: A context-less storyboard keeps the CAP-VIRAL document
    Given a context-less viral storyboard
    When the contextual storyboard is rendered
    Then the contextual storyboard document does not contain "== QQOQCP"

  Scenario: A partially contextualised storyboard is rejected by the coherence gate
    Given a viral storyboard carrying only the QQOQCP axis
    When the contextual storyboard is validated
    Then the contextual storyboard verdict is invalid
    And the contextual verdict reason mentions "partial"

  Scenario: A narrative thread requires an entering transition per sequence
    Given a fully contextual viral storyboard with a missing second transition
    When the contextual storyboard is validated
    Then the contextual storyboard verdict is invalid
    And the contextual verdict reason mentions "transition"
