@multilang @i18n
Feature: Multi-language capsule video pipeline (CAP-29.5)

  As a capsule-gradle producer
  I want a single pipeline that generates one localized capsule video per target language
  So that the full 10-language feed produces <deckName>_<lang>.webm artifacts

  Scenario: The multi-language plan covers all 10 LanguageCatalog languages
    Given translated decks and scripts for all 10 LanguageCatalog languages
    When the multi-language video plan is built
    Then the plan contains 10 entries
    And the plan languages include "ar" and "ur"

  Scenario: The plan names output videos with the deckName_lang.webm convention
    Given translated decks and scripts for languages "fr" and "en"
    When the multi-language video plan is built
    Then the plan entry for "fr" outputs "demo_fr.webm"
    And the plan entry for "en" outputs "demo_en.webm"

  Scenario: Languages without a matching deck and script pair are skipped
    Given translated decks and scripts for languages "fr" and "en"
    When the multi-language video plan is built for "fr", "en" and "ar"
    Then the plan contains 2 entries
    And the plan does not include language "ar"

  Scenario: An empty plan fails fast when the upstream pipeline produced nothing
    Given no translated decks or scripts
    When the multi-language video plan is built for "fr" and "en"
    Then the plan build fails with a message containing "no matching deck + script pair"

  Scenario: The task generates one WebM per language with NoOp engines
    Given a Gradle project with the capsule plugin applied and NoOp engines
    And translated decks and scripts for all 10 LanguageCatalog languages
    When I run the multi-language video generation task
    Then the build output reports 10 rendered languages
    And a video file "demo_fr.webm" is generated in the capsules build directory
    And a video file "demo_ar.webm" is generated in the capsules build directory
    And a video file "demo_ur.webm" is generated in the capsules build directory

  Scenario: RTL languages ar and ur produce their localized WebM
    Given a Gradle project with the capsule plugin applied and NoOp engines
    And translated decks and scripts for RTL languages "ar" and "ur"
    When I run the multi-language video generation task
    Then the build output reports 2 rendered languages
    And a video file "demo_ar.webm" is generated in the capsules build directory
    And a video file "demo_ur.webm" is generated in the capsules build directory
