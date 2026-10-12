@fish
Feature: Fish Audio cloud TTS engine

  The Fish Audio engine is a 4th additive TtsEngine (cloned voice). Tests never
  call the real service: the HTTP transport is injected, so a fake returns canned
  WAV bytes (or a failure status). No API key, no network.

  Scenario: The engine posts the request and writes a take
    Given a Fish Audio engine with a key
    When the engine synthesises "Bonjour le monde"
    Then the request endpoint is "https://api.fish.audio/v1/tts"
    And the request authorization header is "Bearer sk-fish-test"
    And the request model header is "speech-1.6"
    And the request body text is "Bonjour le monde"
    And the request body reference_id is "voice-123"
    And the take is written

  Scenario: Without a key the engine is unavailable
    Given a Fish Audio engine without a key
    When the engine synthesises "Bonjour"
    Then the Fish Audio engine is unavailable
    And synthesising fails with "no API key"

  Scenario: A transient failure is retried then succeeds
    Given a Fish Audio engine with a key
    And the transport returns 503 then a success
    When the engine synthesises "Bonjour"
    Then the transport received 2 requests
    And the take is written

  Scenario: The synthesis fails after the bounded attempts
    Given a Fish Audio engine with a key
    And the transport always returns 500
    When the engine synthesises "Bonjour"
    Then synthesising fails with "failed after 3 attempt"

  Scenario: The key never leaks through the engine rendering
    Given a Fish Audio engine with a key
    Then the engine rendering does not contain the key
