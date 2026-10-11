@viral-gate
Feature: Viral campaign delivery gate

  The campaign manifest references rendered assets by path. Before the campaign
  is handed to the distribution layer (GBL-002), the gate verifies that every
  referenced asset actually exists — a campaign is never declared delivered with
  a missing video.

  Scenario: A campaign whose assets exist passes the gate
    Given a campaign gate manifest with a present video "build/viral/fr-tiktok-30.mp4"
    When the campaign gate is evaluated
    Then the campaign gate verdict is valid

  Scenario: A missing video is reported
    Given a campaign gate manifest with a missing video "build/viral/missing.mp4"
    When the campaign gate is evaluated
    Then the campaign gate verdict is invalid
    And the campaign gate reports "fr/TIKTOK videoFile: build/viral/missing.mp4"

  Scenario: A missing thumbnail is reported
    Given a campaign gate manifest with a present video "build/viral/fr-tiktok-30.mp4"
    And a missing thumbnail "build/viral/missing.png"
    When the campaign gate is evaluated
    Then the campaign gate verdict is invalid
    And the campaign gate reports "fr/TIKTOK thumbnailFile: build/viral/missing.png"

  Scenario: A blank thumbnail is not a violation
    Given a campaign gate manifest with a present video "build/viral/fr-tiktok-30.mp4"
    When the campaign gate is evaluated
    Then the campaign gate verdict is valid

  Scenario: The manifest serialises then parses back to the same bundles
    Given a campaign gate manifest with a present video "build/viral/fr-tiktok-30.mp4"
    When the campaign manifest round-trips
    Then the campaign gate bundles are preserved
