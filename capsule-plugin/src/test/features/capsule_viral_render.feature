@viral-render
Feature: The viral render writes a real short-form MP4

  As a capsule-gradle producer
  I want the fitted short-form render to write a real, exploitable MP4
  So that the deliverable really exists (not just a referenced path)

  Scenario: The fitted durations produce a real MP4 within the target window
    Given a real viral render of beats "3,10,10,7" to target 30 with tolerance 2.0
    Then a real MP4 is produced
    And the real MP4 duration is close to 30

  Scenario: A 15-second target yields a real 15-second MP4
    Given a real viral render of beats "3,9,9,6" to target 15 with tolerance 2.0
    Then a real MP4 is produced
    And the real MP4 duration is close to 15
