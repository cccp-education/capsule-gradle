@audio @post
Feature: Capsule audio post-production (CAP-AUDIO US-4)

  As a capsule-gradle producer
  I want to apply BGM mix, loudness normalization (EBU R128), and sidechain ducking
  So that the final WebM sounds professional (music bed + normalized voice + ducking)

  Scenario: Default audio post config is a no-op skip (economy of ink)
    Given a Capsule audio post Gradle project with the capsule plugin applied
    And a demo deck and script are present for audio post
    When I generate the capsule video with the default audio post config
    Then the audio post build succeeds
    And the audio post output does not mention "Audio post:"
    And an audio post WebM file is produced

  Scenario: BGM enabled with NoOp processor degrades and keeps original WebM
    Given a Capsule audio post Gradle project with the capsule plugin applied
    And a demo deck and script are present for audio post
    When I generate the capsule video with BGM enabled and a NoOp ffmpeg path
    Then the audio post build succeeds
    And the audio post output mentions "Audio post:"
    And an audio post WebM file is produced

  Scenario: Ducking enabled without BGM triggers loudness-only fallback with NoOp degraded
    Given a Capsule audio post Gradle project with the capsule plugin applied
    And a demo deck and script are present for audio post
    When I generate the capsule video with ducking enabled and BGM disabled and a NoOp ffmpeg path
    Then the audio post build succeeds
    And the audio post output mentions "Audio post:"
    And an audio post WebM file is produced

  Scenario: Ducking and BGM enabled with NoOp processor degrades and keeps original WebM
    Given a Capsule audio post Gradle project with the capsule plugin applied
    And a demo deck and script are present for audio post
    When I generate the capsule video with ducking and BGM enabled and a NoOp ffmpeg path
    Then the audio post build succeeds
    And the audio post output mentions "Audio post:"
    And an audio post WebM file is produced