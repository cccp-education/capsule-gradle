@voice-verify
Feature: Voice quality verification (CAP-QC-VOICE US-2)

  As a capsule-gradle producer
  I want to score synthesised takes through the ASR port with fakes
  So that the verification orchestration is testable without any real ASR

  Scenario: A faithful take passes verification
    Given a voice verification take 1 expecting "bonjour le monde"
    And the voice recognition returns "bonjour le monde"
    When the voice verification runs
    Then the voice verification has no failure
    And the voice verification take 1 is PASS

  Scenario: A hallucinated take fails verification
    Given a voice verification take 1 expecting "a b c"
    And the voice recognition returns "a b c d e f"
    When the voice verification runs
    Then the voice verification has 1 failure
    And the voice verification take 1 is FAIL
    And the voice verification report lists reason "surplus"

  Scenario: A recognition error degrades the take without failing
    Given a voice verification take 1 expecting "bonjour le monde"
    And the voice recognition fails
    When the voice verification runs
    Then the voice verification has no failure
    And the voice verification take 1 is DEGRADED

  Scenario: An unavailable ASR degrades the whole verification
    Given a voice verification take 1 expecting "bonjour le monde"
    And the voice recognition is unavailable
    When the voice verification runs
    Then the voice verification has no failure
    And the voice verification is degraded
    And the voice verification take 1 is DEGRADED

  Scenario: A silent take fails even when the ASR is unavailable
    Given a voice verification take 1 expecting "bonjour le monde"
    And the voice verification audio envelope is silent
    And the voice recognition is unavailable
    When the voice verification runs
    Then the voice verification has 1 failure
    And the voice verification report lists reason "silent-audio"
    And the voice verification is degraded

  Scenario: An audible take with an unavailable ASR remains degraded
    Given a voice verification take 1 expecting "bonjour le monde"
    And the voice verification audio envelope is audible
    And the voice recognition is unavailable
    When the voice verification runs
    Then the voice verification has no failure
    And the voice verification take 1 is DEGRADED

  Scenario: The best take is selected among a slide's candidates
    Given a voice verification candidate 1 named "slide-01-1.mp3" expecting "le chat noir" transcribed "le chat noir"
    And a voice verification candidate 1 named "slide-01-2.mp3" expecting "le chat noir" transcribed "le chien blanc"
    When the best voice take is selected
    Then the chosen voice take for slide 1 is "slide-01-1.mp3"
    And the chosen voice take count for slide 1 is 2

  Scenario: A faithful candidate beats a degraded one
    Given a voice verification candidate 1 named "slide-01-1.mp3" expecting "bonjour" transcribed "bonjour"
    And a degraded voice verification candidate 1 named "slide-01-2.mp3"
    When the best voice take is selected
    Then the chosen voice take for slide 1 is "slide-01-1.mp3"
