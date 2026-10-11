@subtitle @cartons
Feature: Subtitle cartons (PRT-03)

  As a capsule-gradle producer
  I want long speaker notes subdivided into readable subtitle cartons with proportional timing
  So that a slide's narration is readable on screen instead of one 400-character cue

  Scenario: A speaker note is normalised for display
    Given the subtitle speaker note "des **classes** et l' A P I"
    When the subtitle speaker note is normalised for display
    Then the subtitle display text equals "des classes et l' API"

  Scenario: A long speaker note is split into readable cartons
    Given the subtitle speaker note "un deux trois quatre cinq six sept huit neuf dix onze"
    When the subtitle speaker note is split into cartons of at most 15 characters
    Then every subtitle carton is at most 15 characters
    And the subtitle cartons rejoin the normalised note

  Scenario: A slide duration is distributed across cartons proportionally
    Given subtitle cartons "aa" and "bbbb"
    When a 6 second take starting at 10 is distributed across the subtitle cartons
    Then the subtitle timing ranges cover 10.0 to 16.0 contiguously
    And the longer subtitle carton lasts longer

  Scenario: One cue per carton is produced across two slides
    Given two subtitle slides with notes "court" and "un deux trois quatre cinq six sept huit"
    When subtitle cues are calculated from cartons of at most 12 characters
    Then the subtitle cue indices are sequential from 1
    And the last subtitle cue ends at 1.0 second
