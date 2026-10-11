@normalize
Feature: Viral storyboard normalization

  A hand-authored storyboard carrying a narrative thread must give every beat an
  entering transition, or the editorial gate rejects it. Normalization fills the
  missing ones from a role-based default policy (an explicit transition is kept).

  Scenario: Missing transitions are filled from the role default
    Given a storyboard to normalize with beats
      | role        | transition |
      | hook        |            |
      | development |            |
      | cta         |            |
    When the storyboard is normalized
    Then the normalized beat 0 transition is "— (ouverture)"
    And the normalized beat 1 transition is "cut"
    And the normalized beat 2 transition is "fade"

  Scenario: An explicit transition is preserved
    Given a storyboard to normalize with beats
      | role        | transition |
      | development | dissolve   |
    When the storyboard is normalized
    Then the normalized beat 0 transition is "dissolve"

  Scenario: An unknown role falls back to cut
    Given a storyboard to normalize with beats
      | role   | transition |
      | teaser |            |
    When the storyboard is normalized
    Then the normalized beat 0 transition is "cut"

  Scenario: Normalization is idempotent
    Given a storyboard to normalize with beats
      | role | transition |
      | hook |            |
      | cta  |            |
    When the storyboard is normalized twice
    Then the two normalizations are equal
