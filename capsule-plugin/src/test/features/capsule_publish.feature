@publish
Feature: HF-YOUTUBE-PUBLISH US-3b — Capsule YouTube publication

  As a capsule producer
  I want the produced MP4 to reach the configured channel through the shared VideoPublisher port
  So that a finished capsule is distributed without a manual step

  Scenario: A successful publish returns the video URL
    Given a capsule publish fake transport returning a token and a session and the video id "VID"
    And a capsule publish credentials file with a refresh token
    When I publish a capsule video from a temp file
    Then the capsule publish result is published
    And the capsule publish url is "https://youtu.be/VID"

  Scenario: Incomplete credentials degrade without any request
    Given a capsule publish fake transport returning a token and a session and the video id "VID"
    And a capsule publish credentials file without a refresh token
    When I publish a capsule video from a temp file
    Then the capsule publish result is degraded
    And the capsule publish transport received no request

  Scenario: A refused token fails
    Given a capsule publish fake transport refusing the token
    And a capsule publish credentials file with a refresh token
    When I publish a capsule video from a temp file
    Then the capsule publish result is failed

  Scenario: Disabled publishing degrades without any request
    Given capsule publishing is disabled
    When I publish a capsule video from a temp file
    Then the capsule publish result is degraded
    And the capsule publish transport received no request

  Scenario: An unsupported platform degrades without any request
    Given a capsule publish fake transport returning a token and a session and the video id "VID"
    And a capsule publish credentials file with a refresh token
    And the capsule publish platform is "vimeo"
    When I publish a capsule video from a temp file
    Then the capsule publish result is degraded
    And the capsule publish transport received no request
