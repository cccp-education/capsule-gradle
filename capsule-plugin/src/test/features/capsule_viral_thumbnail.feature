@thumbnail
Feature: Viral campaign cover

  The campaign cover is the hook frame the distribution layer shows before the
  video plays. It is planned from the storyboard (hook beat, else message) and
  rendered as a capturable portrait document.

  Scenario: The cover carries the hook beat on a portrait document
    Given a viral thumbnail for a storyboard with a hook beat "Et si 30 secondes suffisaient ?"
    When the viral cover is built
    Then the viral cover is a portrait 1080x1920 document
    And the viral cover carries "Et si 30 secondes suffisaient ?"

  Scenario: A storyboard without a hook beat falls back to the message
    Given a viral thumbnail for a storyboard with no hook beat
    When the viral cover is built
    Then the viral cover carries "Devenir développeur professionnel"

  Scenario: The cover escapes html in the overlay
    Given a viral thumbnail whose hook carries html "<b>gras</b>"
    When the viral cover is built
    Then the viral cover escapes html

  Scenario: The cover file name follows the per-variant convention
    Then the viral cover file name for "fr-tiktok-30" is "fr-tiktok-30-thumb.png"
