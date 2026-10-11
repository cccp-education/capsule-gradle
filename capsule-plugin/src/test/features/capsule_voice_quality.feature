@voice
Feature: Voice quality pure domain (CAP-QC-VOICE US-1)

  As a capsule-gradle producer
  I want to score a synthesised take against its expected text without any ASR
  So that the fidelity logic is testable on its own before the ASR port is wired

  Scenario: Normalisation folds case, accents and punctuation
    Given the voice narration "Été — Cœur!"
    When the voice narration is normalised
    Then the normalised voice words are "ete,coeur"

  Scenario: An exact transcription scores a zero word error rate
    Given the expected voice narration "bonjour le monde"
    And the transcribed voice narration "bonjour le monde"
    When the voice fidelity is scored
    Then the voice word error rate is 0.0
    And the voice surplus word count is 0

  Scenario: Extra transcribed words are a positive surplus
    Given the expected voice narration "a b c"
    And the transcribed voice narration "a b c d"
    When the voice fidelity is scored
    Then the voice surplus word count is 1

  Scenario: A substitution gives a positive word error rate
    Given the expected voice narration "le chat noir"
    And the transcribed voice narration "le chien noir"
    When the voice fidelity is scored
    Then the voice word error rate is greater than 0.0
