@ai
Feature: Capsule LLM bridge via codebase (CAP-ARCH-1)

  As a capsule-gradle producer
  I want a codebase-backed LLM socle (LlmBuildService + ChatModel adapter)
  So that capsule content generation is driven by the N1 codebase hub

  Scenario: The capsule AI smoke test task is registered
    Given a Capsule AI Gradle project with the capsule plugin applied
    When I list the Gradle tasks
    Then the task "capsuleAiSmokeTest" is listed in the output

  Scenario: The smoke test calls a mock LLM and logs the response
    Given a Capsule AI Gradle project with the capsule plugin applied
    And a mock Ollama LLM server answering "capsule-ai-ok"
    When I run the capsule AI smoke test against the mock LLM
    Then the build succeeds
    And the build output logs "CAPSULE AI"
    And the build output logs the mock response "capsule-ai-ok"

  Scenario: The smoke test fails fast when the LLM is unreachable
    Given a Capsule AI Gradle project with the capsule plugin applied
    When I run the capsule AI smoke test without a reachable LLM
    Then the build fails when the LLM is unreachable
