@audiodesc @a11y
Feature: CAP-AUDIODESC — audio description of the video

  As a video author
  I want the content that is only seen to be described in the narration silences
  So that a blind learner can follow the capsule

  Scenario: A cue is placed into a narration silence
    Given a narration with two windows of 2 and 3 seconds separated by 2 seconds
    And a description cue "A diagram appears" lasting 1.5 seconds
    When I plan the audio description
    Then the plan places 1 cue at 2 seconds

  Scenario: Cues that do not fit are declared
    Given a narration with two windows of 2 and 3 seconds separated by 0.1 seconds
    And a description cue "A very long description" lasting 5 seconds
    When I plan the audio description
    Then the plan declares 1 unplaced cue
    And the limits are declared

  Scenario: The mix ducks the description under the narration
    Given a planned cue at 1.0 seconds
    When I build the audio description mix
    Then the mix delays the cue to 1000 milliseconds
    And the mix ducks the description with a sidechain

  Scenario: A complete accessible deliverable is accepted
    Given subtitles, a transcript and an audio description
    When I evaluate accessibility
    Then the verdict is accepted
    And the limits are declared

  Scenario: A missing transcription degrades the verdict
    Given subtitles and an audio description but no transcript
    When I evaluate accessibility
    Then the verdict is degraded
    And the degradation names the transcription

  Scenario: A missing audio description degrades the verdict
    Given a requested audio description that is absent
    When I evaluate accessibility
    Then the verdict is degraded
    And the degradation names the audio description
