@viral
Feature: Viral campaign engine

  As a capsule-gradle producer
  I want to turn a training product into a short-form vertical campaign
  So that talaria and cccp.education can be promoted on YouTube/TikTok/Reels

  Background:
    Given a viral storyboard for deck "fpa-decouverte" in language "fr" on platform "TIKTOK"
      | role        | intent                        | duration | type |
      | hook        | Et si 30 secondes suffisaient ? | 3        | HTML |
      | development | Les 4 activités types           | 10       | HTML |
      | development | Le rôle du formateur            | 10       | HTML |
      | cta         | Découvrez FPA                   | 7        | HTML |

  Scenario: A complete storyboard passes the editorial gate
    When the storyboard is validated
    Then the storyboard verdict is valid

  Scenario: A storyboard without a hook is rejected
    Given a viral storyboard for deck "fpa-decouverte" in language "fr" on platform "TIKTOK"
      | role        | intent          | duration | type |
      | development | Dev             | 25       | HTML |
      | cta         | CTA             | 5        | HTML |
    When the storyboard is validated
    Then the storyboard verdict is invalid
    And the storyboard reason mentions "hook"

  Scenario: The storyboard renders a deterministic AsciiDoc document
    When the storyboard is rendered
    Then the storyboard document contains "= Storyboard — Devenir formateur professionnel"
    And the storyboard document contains ":platform: TIKTOK"

  Scenario: The batch matrix produces one variant per language and platform
    Given a viral batch with languages "fr,en" platforms "TIKTOK,YOUTUBE_SHORT" and duration 30
    Then the batch contains 4 variants
    And the batch variant ids are "fr-tiktok-30,fr-youtube_short-30,en-tiktok-30,en-youtube_short-30"

  Scenario: The context-anchored hook prompt embeds the augmented context
    Given a viral hook plan for deck "fpa-decouverte" in language "fr" with augmented context:
    """
    ==== Docs (DOCS)
    Le référentiel FPA définit 4 activités types.
    """
    When the viral hook prompt is built
    Then the viral hook prompt contains "Le référentiel FPA définit 4 activités types."
    And the viral hook prompt forbids inventing facts

  Scenario: The campaign bundles read the market copy and degrade gracefully
    Given a viral batch with languages "fr" platforms "TIKTOK" and duration 30
    And a market copy for "fr" and "TIKTOK" with title "Titre natif"
    When the campaign is assembled with a rendered video
    Then the campaign contains 1 bundle
    And the first campaign bundle title is "Titre natif"

  Scenario: A missing market copy degrades to the storyboard message
    Given a viral batch with languages "fr" platforms "TIKTOK" and duration 30
    And no market copy is available
    When the campaign is assembled with a rendered video and no hook
    Then the first campaign bundle title is "Devenir formateur professionnel"

  Scenario: A rendered storyboard parses back into the same document
    When the storyboard is rendered then parsed back
    Then the parsed storyboard equals the original

  Scenario: The storyboard renders a capturable portrait deck
    When the storyboard is rendered as a deck
    Then the viral deck is a portrait 1080x1920 document
    And the viral deck has 4 sections
    And the viral deck contains "data-duration=\"3.0\""

  Scenario: The campaign manifest serialises the bundles as a stable JSON array
    Given a viral batch with languages "fr" platforms "TIKTOK" and duration 30
    And a market copy for "fr" and "TIKTOK" with title "Titre natif"
    When the campaign is assembled with a rendered video
    Then the campaign manifest is a JSON array of 1 bundle
    And the campaign manifest first bundle title is "Titre natif"
