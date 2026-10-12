@kernel-model
Feature: Engine-neutral video model
  The kernel speaks segment/visual/engine, never slide/deck/reveal. The legacy
  feed names remain as additive typealiases so the mature plugin keeps compiling
  while new code speaks the neutral vocabulary.

  Scenario: a narration-only segment defaults to a visual kind
    Given a kernel segment titled "Opening" narrated "Hello."
    Then the kernel segment kind is "VISUAL"
    And the kernel segment carries no scene

  Scenario: an animation segment carries its scene
    Given a kernel animation segment titled "Diagram" with scene "MoveSquare"
    Then the kernel segment kind is "ANIMATION"
    And the kernel segment scene is "MoveSquare"

  Scenario: the legacy slide type maps to the neutral video kind
    Given a legacy slide type "HTML"
    Then its neutral video kind is "VISUAL"
    And the legacy slide type "MANIM" maps to the neutral kind "ANIMATION"

  Scenario: the legacy typealias bridge exposes the neutral kind
    Given a legacy slide segment aliased to the neutral kind "ANIMATION"
    Then its legacy accessor type is "MANIM"

  Scenario: a production entry exposes its visual source as a legacy deck file
    Given a kernel production entry backed by the visual file "demo_en-deck.html"
    Then its legacy deck file name is "demo_en-deck.html"
