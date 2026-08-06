@parallel @timeout
Feature: Parallel capture timeout handling

  As a capsule-gradle producer
  I want slide captures to be bounded by a timeout and degraded gracefully
  So that a stuck or failing capture cannot hang or crash the whole deck

  Scenario: A blocking capture times out instead of hanging the build
    Given a deck with 2 slides
    And a capture factory that blocks indefinitely
    When parallel capture runs with a timeout of 1000 ms
    Then the capture aborts with a timeout

  Scenario: A failing slide is reported as failed and does not stop the other slides
    Given a deck with 4 slides
    And a capture factory where slides 1 and 3 throw
    When parallel capture runs with a timeout of 10000 ms
    Then 2 slides are reported as failed
    And slide 2 webm is produced
    And slide 4 webm is produced
