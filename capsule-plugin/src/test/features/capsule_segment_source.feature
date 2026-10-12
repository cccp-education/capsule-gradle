@segment-source
Feature: Segment source port
  A source file yields narrated segments through the engine-neutral SegmentSource
  port; the reveal.js adapter owns the speaker-notes parsing and the -script.txt
  contract.

  Scenario: an AsciiDoc deck yields one segment per narrated slide
    Given a reveal AsciiDoc deck named "demo" with narrated slides "Intro" and "Closing"
    When the reveal speaker-note source reads the deck file
    Then the segment source yields 2 segments
    And the first segment source title is "Intro"

  Scenario: slides without a speaker note are skipped
    Given a reveal AsciiDoc deck named "demo" with narrated slides "Intro" and "Closing"
    And the reveal deck "demo" also contains a silent slide "Silent"
    When the reveal speaker-note source reads the deck file
    Then the segment source yields 2 segments

  Scenario: the script contract round-trips through the deck format
    Given a video script named "demo" with 2 segments
    When the script is written and read back by the reveal contract
    Then the reveal read script name is "demo"
    And the reveal read script has 2 segments
