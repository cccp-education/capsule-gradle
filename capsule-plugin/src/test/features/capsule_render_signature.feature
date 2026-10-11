@capture @signature
Feature: Render signature (PRT-06)

  As a capsule-gradle producer
  I want a stable fingerprint of a captured clip
  So that a clip whose inputs are unchanged is reused instead of re-rendered

  Scenario: The signature is stable for identical inputs
    Given a render signature built from engine "screenshot" content "deck" and timeline "5.0"
    And a render signature built from engine "screenshot" content "deck" and timeline "5.0"
    Then the two render signatures are equal

  Scenario: A changed speaker note changes the signature
    Given a render signature built from engine "screenshot" content "deck-v1" and timeline "5.0"
    And a render signature built from engine "screenshot" content "deck-v2" and timeline "5.0"
    Then the two render signatures differ

  Scenario: The signature is a fixed-length lowercase hexadecimal string
    Given a render signature built from engine "remotion" content "deck" and timeline "5.0"
    Then the render signature is 16 lowercase hexadecimal characters
