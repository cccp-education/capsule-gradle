@timeline
Feature: Deterministic timeline (PRT-07)

  As a capsule-gradle producer
  I want per-slide durations snapped to a contiguous integer-frame timeline
  So that sound and image cannot drift, however long the capsule is

  Scenario: Durations snap to a contiguous integer-frame timeline
    Given a timeline with durations "5.0,3.0" at 30 fps
    When the timeline is built
    Then the timeline totals 240 frames
    And the timeline totals 8.0 seconds
    And timeline slide 0 starts at frame 0 with 150 frames
    And timeline slide 1 starts at frame 150 with 90 frames

  Scenario: Lead and tail pad the timeline
    Given a timeline with durations "1.0" at 30 fps
    And the timeline lead is 0.5 seconds and tail is 0.5 seconds
    When the timeline is built
    Then the timeline totals 60 frames

  Scenario: The avance makes the visual lead the voice on later slides
    Given a timeline with durations "1.0,1.0" at 30 fps
    And the timeline avance is 0.1 seconds
    When the timeline is built
    Then timeline slide 1 voice starts at frame 33
