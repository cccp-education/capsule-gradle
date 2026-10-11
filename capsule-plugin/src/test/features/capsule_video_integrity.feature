@validation @integrity
Feature: Capsule video integrity validation (PRT-04)

  As a capsule-gradle producer
  I want the produced media to actually DECODE, not merely exist
  So that a broken container (valid duration, unreadable content) never passes for reusable

  Scenario: Integrity validation disabled by default is a no-op skip
    Given a capsule video integrity project with a corrupt video "demo.webm"
    When the validateCapsuleVideoDuration task is run with integrity validation disabled
    Then the video integrity build output contains "skipped"
    And the video integrity build does not fail

  Scenario: Integrity validation enabled with no video file skips gracefully
    Given a capsule video integrity project with no video file
    When the validateCapsuleVideoDuration task is run with integrity validation enabled
    Then the video integrity build output contains "not found"
    And the video integrity build does not fail

  Scenario: Integrity validation enabled with a corrupt video fails the build
    Given a capsule video integrity project with a corrupt video "demo.webm"
    When the validateCapsuleVideoDuration task is run with integrity validation enabled and the build is expected to fail
    Then the video integrity build fails
    And the video integrity build output contains "Video integrity check failed"
