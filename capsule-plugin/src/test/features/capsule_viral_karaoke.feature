@karaoke
Feature: Viral karaoke captions

  The short-form video carries word-by-word karaoke captions: each storyboard
  beat is one line, its words highlighted in turn by the ASS `\k` override.

  Scenario: The karaoke timeline covers each beat window
    Given a karaoke timeline for beats
      | intent           | seconds |
      | Bonjour le monde | 3       |
      | Au revoir        | 2       |
    Then the karaoke timeline has 2 lines
    And the karaoke timeline spans 0 to 5

  Scenario: A beat is split into word-by-word highlights
    Given a karaoke timeline for beats
      | intent           | seconds |
      | Bonjour le monde | 3       |
    Then the karaoke first line has 3 words
    And the karaoke first line words are "Bonjour,le,monde"

  Scenario: The ASS document carries a karaoke dialogue per line
    Given a karaoke timeline for beats
      | intent           | seconds |
      | Bonjour le monde | 3       |
      | Au revoir        | 2       |
    When the karaoke ASS document is built
    Then the karaoke document is an ASS v4.00+ document
    And the karaoke document has 2 dialogues
    And the karaoke document contains a karaoke override

  Scenario: A single-word beat fills its whole window
    Given a karaoke timeline for beats
      | intent  | seconds |
      | Bonjour | 2       |
    Then the karaoke first line has 1 words
    And the karaoke timeline spans 0 to 2

  Scenario: Measured ASR timings drive the word highlights
    Given karaoke beats
      | intent           | seconds |
      | Bonjour le monde | 3       |
    And karaoke word timings
      | word    | start | end |
      | bonjour | 0.1   | 0.6 |
      | le      | 0.7   | 0.9 |
      | monde   | 1.0   | 1.8 |
    When the karaoke timeline is planned with timings
    Then the karaoke first line first word starts at 0.1
    And the karaoke first line last word ends at 1.8
