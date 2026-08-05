@capsule @translation @pipeline
Feature: Translate and extract speaker notes pipeline (SLD-11.4)

  As a capsule-gradle producer
  I want a single composite task that translates a deck and extracts speaker notes
  So that the multi-language capsule feed pipeline is a one-command operation

  Scenario: The composite task name is stable
    When the translateAndExtractSpeakerNotes task name is read
    Then the task name should be "translateAndExtractSpeakerNotes"

  Scenario: The composite task description mentions both sub-tasks
    When the translateAndExtractSpeakerNotes task description is read
    Then the description should mention "translateDeck"
    And the description should mention "extractSpeakerNotes"

  Scenario: The composite task group is capsule
    When the translateAndExtractSpeakerNotes task group is read
    Then the task group should be "capsule"