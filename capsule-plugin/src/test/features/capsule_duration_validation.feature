@validation @duration
Feature: Capsule video duration validation (CAP-CR3-1 US-3)

  As a capsule-gradle producer
  I want to validate the final video duration against the sum of TTS audio durations
  So that a "check green" guarantees an exploitable video (no silent truncation or misalignment)

  Scenario: Duration validation disabled by default is a no-op skip
    Given a capsule duration validation project with no video and no audio files
    When the validateCapsuleVideoDuration task is run with duration validation disabled
    Then the duration validation build output contains "skipped"
    And the duration validation build does not fail

  Scenario: Duration validation enabled with no video file skips gracefully
    Given a capsule duration validation project with no video and no audio files
    When the validateCapsuleVideoDuration task is run with duration validation enabled
    Then the duration validation build output contains "not found"
    And the duration validation build does not fail

  Scenario: Duration validation enabled with valid video and matching audio succeeds
    Given a capsule duration validation project with a fake video "demo.webm" and 2 fake audio files in the build output directory
    When the validateCapsuleVideoDuration task is run with duration validation enabled and tolerance "2.0"
    Then the duration validation build output contains "CAPSULE VALIDATION"
    And the duration validation build does not fail