@multilang @i18n
Feature: Multi-language capsule video pipeline (CAP-29.5)

  As a capsule-gradle producer
  I want a single pipeline that generates one localized capsule video per target language
  So that the full multi-language feed produces <deckName>_<lang>.webm artifacts

  Scenario: The multi-language plan covers all LanguageCatalog languages
    Given translated decks and scripts for all LanguageCatalog languages
    When the multi-language video plan is built
    Then the plan contains all LanguageCatalog entries
    And the plan languages include "ar" and "ur"

  Scenario: The plan names output videos with the deckName_lang.webm convention
    Given translated decks and scripts for languages "fr" and "en"
    When the multi-language video plan is built
    Then the plan entry for "fr" outputs "demo_fr.webm"
    And the plan entry for "en" outputs "demo_en.webm"

  Scenario: The real slider-to-capsule chain script carries a -deck suffix and is still resolved
    Given translated decks and real-chain scripts carrying the -deck suffix for language "fr"
    When the multi-language video plan is built
    Then the plan contains 1 entries
    And the plan entry for "fr" resolves the script file "demo_fr-deck-script.txt"
    And the plan entry for "fr" outputs "demo_fr.webm"

  Scenario: Languages without a matching deck and script pair are skipped
    Given translated decks and scripts for languages "fr" and "en"
    When the multi-language video plan is built for "fr", "en" and "ar"
    Then the plan contains 2 entries
    And the plan does not include language "ar"

  Scenario: An empty plan fails fast when the upstream pipeline produced nothing
    Given no translated decks or scripts
    When the multi-language video plan is built for "fr" and "en"
    Then the plan build fails with a message containing "no matching deck + script pair"

  # The former single "all languages" scenario is split into four language-group
  # partitions (the planner skips languages without a deck+script pair), so the
  # heavy TestKit generation runs on 4 CI machines in parallel.
  @multilang-gen @multilang-gen-a
  Scenario: The task generates one WebM per language — language group A
    Given a Gradle project with the capsule plugin applied and NoOp engines
    And translated decks and scripts for language group "a"
    When I run the multi-language video generation task
    Then the build output reports the whole language group "a" rendered

  @multilang-gen @multilang-gen-b
  Scenario: The task generates one WebM per language — language group B
    Given a Gradle project with the capsule plugin applied and NoOp engines
    And translated decks and scripts for language group "b"
    When I run the multi-language video generation task
    Then the build output reports the whole language group "b" rendered

  @multilang-gen @multilang-gen-c
  Scenario: The task generates one WebM per language — language group C
    Given a Gradle project with the capsule plugin applied and NoOp engines
    And translated decks and scripts for language group "c"
    When I run the multi-language video generation task
    Then the build output reports the whole language group "c" rendered

  @multilang-gen @multilang-gen-d
  Scenario: The task generates one WebM per language — language group D
    Given a Gradle project with the capsule plugin applied and NoOp engines
    And translated decks and scripts for language group "d"
    When I run the multi-language video generation task
    Then the build output reports the whole language group "d" rendered

  Scenario: RTL languages ar and ur produce their localized WebM
    Given a Gradle project with the capsule plugin applied and NoOp engines
    And translated decks and scripts for RTL languages "ar" and "ur"
    When I run the multi-language video generation task
    Then the build output reports 2 rendered languages
    And a video file "demo_ar.webm" is generated in the capsules build directory
    And a video file "demo_ur.webm" is generated in the capsules build directory
