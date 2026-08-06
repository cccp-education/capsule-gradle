@model @boundary
Feature: Consolidated capsule model round-trip

  As a capsule-gradle producer
  I want a single consolidated feed model that survives a write-then-read round-trip
  So that slide type and manim scene metadata are preserved across the .txt contract

  Scenario: Round-trip preserves HTML-only deck
    Given a capsule script with deck name "intro" and segments
      | index | title   | speakerNote | type |
      | 1     | Welcome | Hello.      | HTML |
    When the script is written to a file and read back
    Then the read script deck name should be "intro"
    And the read script should contain 1 segment
    And the segment 1 should have type "HTML"
    And the segment 1 should have manimScene "null"

  Scenario: Round-trip preserves MANIM slide with scene name
    Given a capsule script with deck name "anim" and segments
      | index | title  | speakerNote | type  | manimScene |
      | 1     | Intro  | Note.       | HTML  |            |
      | 2     | Anim   | Watch.      | MANIM | MoveSquare |
    When the script is written to a file and read back
    Then the read script should contain 2 segments
    And the segment 1 should have type "HTML"
    And the segment 2 should have type "MANIM"
    And the segment 2 should have manimScene "MoveSquare"
    And the read segment 2 should have title "Anim"

  Scenario: Round-trip preserves multi-line speaker notes
    Given a capsule script with deck name "multi" and segments
      | index | title | speakerNote   | type |
      | 1     | Intro | Line one.     | HTML |
    When the script is written to a file and read back
    Then the read segment 1 should have speakerNote "Line one."

  Scenario: Reading a script with no slide markers yields empty segments
    Given a raw script file "empty-script.txt" with content
      """
      === CAPSULE SCRIPT : empty ===
      """
    When the file is read as a capsule script
    Then the read script deck name should be "empty"
    And the read script should contain 0 segments

  Scenario: Reading a script falls back to filename when header is missing
    Given a raw script file "mydeck-script.txt" with content
      """
      --- SLIDE 1 : Intro ---
      Note.
      """
    When the file is read as a capsule script
    Then the read script deck name should be "mydeck-script"
    And the read script should contain 1 segment
    And the read segment 1 should have title "Intro"