@capsule @architecture @wiring
Feature: Generate capsule content and videos pipeline (CAP-ARCH-4)

  As a capsule-gradle producer
  I want a single composite task that orchestrates LLM content generation and the multi-language video pipeline
  So that enriched speaker notes and localized capsule WebMs are produced in one command

  Scenario: The composite task name is stable
    When the generateCapsuleContentAndVideos task name is read
    Then the content and videos task name should be "generateCapsuleContentAndVideos"

  Scenario: The composite task description mentions both sub-tasks
    When the generateCapsuleContentAndVideos task description is read
    Then the content and videos description should mention "generateCapsuleContent"
    And the content and videos description should mention "translateAndGenerateCapsuleVideos"

  Scenario: The composite task group is capsule
    When the generateCapsuleContentAndVideos task group is read
    Then the content and videos task group should be "capsule"