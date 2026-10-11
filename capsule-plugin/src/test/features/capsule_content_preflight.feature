@qc @preflight
Feature: Content pre-flight (PRT-05)

  As a capsule-gradle producer
  I want a structural pre-flight of the scripts before any metered TTS synthesis
  So that an incoherent deck never costs a paid TTS call

  Scenario: A clean deck yields no finding
    Given a preflight slide 1 with note "hello"
    And a preflight slide 2 with note "world"
    When the content preflight runs
    Then the preflight has no finding

  Scenario: A duplicated note is a warning
    Given a preflight slide 1 with note "same"
    And a preflight slide 2 with note "same"
    When the content preflight runs
    Then the preflight has 1 finding with code "duplicate-note" and severity "WARNING"

  Scenario: Markdown emphasis left in the narrated text is an error
    Given a preflight slide 1 with note "des **classes**"
    When the content preflight runs
    Then the preflight has 1 finding with code "forbidden-form" and severity "ERROR"
