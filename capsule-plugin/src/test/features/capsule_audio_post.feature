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

  Scenario: Two-pass loudness chain injects the measured values (PRT-09)
    Given a measured loudness input of -23.03 -2.19 4.30 -33.31 0.21
    When the two-pass loudness chain is built for target -14.0
    Then the loudness chain contains "loudnorm=I=-14.0:TP=-2:LRA=11"
    And the loudness chain contains "measured_I=-23.03"
    And the loudness chain contains "alimiter=limit=0.63:level=disabled"
    And the loudness chain contains "aresample=48000"

  Scenario: Loudnorm measurement is parsed from the ffmpeg output block (PRT-09)
    Given the loudnorm ffmpeg output block
    When the loudnorm measurement is parsed
    Then the parsed loudness input_i is -23.03
    And the parsed loudness target_offset is 0.21