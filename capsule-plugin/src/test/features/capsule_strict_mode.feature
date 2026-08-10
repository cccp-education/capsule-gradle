@strict @config
Feature: Strict mode anti-NoOp (CAP-CR3-2)

  As a capsule-gradle producer
  I want the build to fail when a required tool is missing in strict mode
  So that I never ship a placeholder video thinking it is real

  Scenario: Strict mode disabled by default preserves the NoOp fallback
    Given a capsule strict project with ttsEngine "piper" and piperExecutablePath "/nonexistent/piper-xyz"
    And strict mode is disabled
    And a capsule script "strict-demo" with one slide
    When the generateCapsule task is run
    Then the strict build succeeds
    And the strict build output contains "TTS engine: noop"

  Scenario: Strict mode enabled fails the build when piper is not available
    Given a capsule strict project with ttsEngine "piper" and piperExecutablePath "/nonexistent/piper-xyz"
    And strict mode is enabled
    And a capsule script "strict-demo" with one slide
    When the generateCapsule task is run
    Then the strict build fails
    And the strict build output contains "strictMode"
    And the strict build output contains "piper"

  Scenario: Strict mode enabled accepts explicit noop engine
    Given a capsule strict project with ttsEngine "noop" and piperExecutablePath "/nonexistent/piper-xyz"
    And strict mode is enabled
    And a capsule script "strict-demo" with one slide
    When the generateCapsule task is run
    Then the strict build succeeds
    And the strict build output contains "TTS engine: noop"

  Scenario: Strict mode enabled via CLI flag fails the build when piper is not available
    Given a capsule strict project with ttsEngine "piper" and piperExecutablePath "/nonexistent/piper-xyz"
    And a capsule script "strict-demo" with one slide
    When the generateCapsule task is run with CLI flag "-Pcapsule.strictMode.enabled=true"
    Then the strict build fails
    And the strict build output contains "strictMode"