@remotion-anim
Feature: Remotion animation timing (CAP-ANIM-TEST)

  As a capsule-gradle maintainer
  I want the Remotion timing logic covered by automated tests
  So that a silent regression of beats, playback rate or cross-fade never ships

  Scenario: Block beats resolve from the JS scale
    Given a remotion anim timing model
    When the beat of block "rule" is requested
    Then the beat seconds is 0.5
    And the beat of block "stat" is 4.5
    And the beat of carried block "h1" is 0.0
    And the beat of unknown block "figure" is 1.6

  Scenario: Manim clip playback rate is bounded
    Given a remotion anim timing model
    When a 40-frame clip is fitted to a 40-frame slide
    Then the playback rate is 1.0
    And a 100-frame clip fitted to a 40-frame slide is clamped to 1.25
    And a 20-frame clip fitted to a 40-frame slide is clamped to 0.8
    And a missing clip fitted to a 40-frame slide plays at 1.0

  Scenario: Cross-fade overlap keeps the screen covered
    Given a remotion anim timing model
    When a two-slide composition of 80 and 60 frames is swept
    Then every frame is fully covered
    And the incoming title only appears once its background covers the outgoing slide
