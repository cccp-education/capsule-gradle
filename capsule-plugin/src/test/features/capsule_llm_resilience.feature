@resilience
Feature: LLM resilience

  A metered cloud LLM fails transiently (429/503, timeout). The capsule adapters
  that own a safe fallback degrade on such an error instead of failing the build —
  the failure is logged, never silent.

  Scenario: A successful guarded call returns its result
    When a guarded LLM call returns "hello"
    Then the guarded result is "hello"

  Scenario: A failing guarded call returns the fallback
    When a guarded LLM call fails with "too many concurrent requests"
    Then the guarded result is "fallback"

  Scenario: The viral hook generator degrades to null on a failing model
    Given a failing chat model
    When the viral hook generator runs
    Then the viral hook is absent
