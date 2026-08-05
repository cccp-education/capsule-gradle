@capsule @feed
Feature: Capsule speaker notes extraction from AsciiDoc decks

  As a capsule-gradle producer
  I want to extract speaker notes from an AsciiDoc deck
  So that the feed pipeline can narrate each slide as a video segment

  Scenario: A deck with one slide having a speaker note produces a one-segment script
    Given an AsciiDoc deck "demo" with content
      """
      = Deck
      author

      == Intro

      [NOTE.speaker]
      --
      Welcome to demo.
      --
      """
    When the capsule script is generated from the deck
    Then the script deck name should be "demo"
    And the script should contain 1 segment
    And the segment 1 should have title "Intro"
    And the segment 1 should have speakerNote "Welcome to demo."

  Scenario: A deck with multiple slides produces one segment per slide in order
    Given an AsciiDoc deck "course" with content
      """
      = Deck
      author

      == Intro

      [NOTE.speaker]
      --
      Welcome.
      --

      == Topic

      [NOTE.speaker]
      --
      Today we cover X.
      --

      == End

      [NOTE.speaker]
      --
      Thank you.
      --
      """
    When the capsule script is generated from the deck
    Then the script should contain 3 segments
    And the segment 1 should have title "Intro"
    And the segment 2 should have title "Topic"
    And the segment 3 should have title "End"
    And the segment 1 should have index 1
    And the segment 2 should have index 2
    And the segment 3 should have index 3

  Scenario: Slides without a speaker note block are skipped
    Given an AsciiDoc deck "mixed" with content
      """
      = Deck
      author

      == Kept

      [NOTE.speaker]
      --
      Has a note.
      --

      == Skipped

      no speaker note here
      """
    When the capsule script is generated from the deck
    Then the script should contain 1 segment
    And the segment 1 should have title "Kept"

  Scenario: The generated script respects the capsule-gradle plain-text contract
    Given an AsciiDoc deck "contract" with content
      """
      = Deck
      author

      == Intro

      [NOTE.speaker]
      --
      Hello world.
      --

      == Topic

      [NOTE.speaker]
      --
      Deep dive.
      --
      """
    When the capsule script is generated from the deck
    And the script is rendered as plain text
    Then the first line should be "=== CAPSULE SCRIPT : contract ==="
    And the second line should be blank
    And the text should contain "--- SLIDE 1 : Intro ---"
    And the text should contain "--- SLIDE 2 : Topic ---"
    And the text should contain "Hello world."
    And the text should contain "Deep dive."

  Scenario: Multi-line speaker notes are preserved verbatim
    Given an AsciiDoc deck "multiline" with content
      """
      = Deck
      author

      == Intro

      [NOTE.speaker]
      --
      Line one.
      Line two.
      --
      """
    When the capsule script is generated from the deck
    And the script is rendered as plain text
    Then the text should contain "Line one."
    And the text should contain "Line two."

  Scenario: A deck with no speaker notes at all yields an empty script
    Given an AsciiDoc deck "empty" with content
      """
      = Deck
      author

      == Slide A

      no speaker note here

      == Slide B

      no speaker note here either
      """
    When the capsule script is generated from the deck
    Then the script should be empty

  Scenario: Generating a script from a blank deck name is rejected
    Given a blank deck name
    When the capsule script generation is attempted
    Then the generation should fail with a message containing "deckName"

  Scenario: A speaker note block with attributes after the bracket is still parsed
    Given an AsciiDoc deck "attrs" with content
      """
      = Deck
      author

      == Intro

      [NOTE.speaker,style=emphasis]
      --
      Has attributes.
      --
      """
    When the capsule script is generated from the deck
    Then the script should contain 1 segment
    And the segment 1 should have speakerNote "Has attributes."

  Scenario: Only level-2 headings are treated as slides
    Given an AsciiDoc deck "levels" with content
      """
      = Deck
      author

      == Real

      [NOTE.speaker]
      --
      Top level note.
      --

      === Sub

      [NOTE.speaker]
      --
      Sub note should be ignored.
      --
      """
    When the capsule script is generated from the deck
    Then the script should contain 1 segment
    And the segment 1 should have title "Real"

  Scenario: Dogfood fr demo deck produces a valid capsule script
    Given the real demo deck "capsule-feed-demo-fr-deck" is loaded
    When the capsule script is generated from the deck
    Then the script should contain 4 segments
    And the segment 1 should have title "Introduction"
    And the segment 4 should have title "Conclusion"

  Scenario: Dogfood en demo deck produces a valid capsule script
    Given the real demo deck "capsule-feed-demo-en-deck" is loaded
    When the capsule script is generated from the deck
    Then the script should contain 4 segments
    And the segment 1 should have title "Introduction"
    And the segment 4 should have title "Conclusion"

  Scenario: Dogfood ar demo deck produces a valid capsule script
    Given the real demo deck "capsule-feed-demo-ar-deck" is loaded
    When the capsule script is generated from the deck
    Then the script should contain 4 segments
    And the segment 1 should have index 1
    And the segment 4 should have index 4

  Scenario: All 3 demo decks render to a contract-compliant plain-text script
    Given the real demo deck "capsule-feed-demo-fr-deck" is loaded
    When the capsule script is generated from the deck
    And the script is rendered as plain text
    Then the first line should be "=== CAPSULE SCRIPT : capsule-feed-demo-fr-deck ==="
    And the second line should be blank
    And the text should contain "--- SLIDE 1 : Introduction ---"