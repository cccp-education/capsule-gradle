@capsule @transcript
Feature: Transcript generation for capsules

  As a capsule-gradle producer
  I want to generate a transcript from speaker notes and slide content
  So that I can provide accessibility and SEO benefits

Scenario: Transcript generation with TEMPLATE strategy (no LLM)
     Given a transcript plan for deck "demo" with language "fr" and strategy TEMPLATE
     And the plan contains slide segments:
       | title   | speakerNote               |
       | Intro   | Welcome to the demo.      |
       | Topic   | Today we discuss X.       |
       | Outro   | Thank you for watching.   |
     When the transcript is generated
     Then the transcript should contain:
     """
     = demo
     :language: fr

     This article is the transcript of the training capsule.

     == Intro

     Welcome to the demo.

     == Topic

     Today we discuss X.

     == Outro

     Thank you for watching.

     End of the capsule transcript.
     """

  Scenario: Transcript generation with LLM strategy (mocked)
    Given a transcript plan for deck "demo" with language "en" and strategy LLM
    And the plan contains slide segments:
      | title   | speakerNote               |
      | Intro   | Welcome to the demo.      |
      | Topic   | Today we discuss X.       |
    When the transcript is generated with LLM enhancement
    Then the transcript should contain enhanced content for each slide
    And the enhanced content should include the original speaker notes
    And the enhanced content should be in English

Scenario: Empty transcript plan yields empty transcript
     Given an empty transcript plan for deck "empty"
     When the transcript is generated
     Then the transcript should be:
     """
     = empty
     :language: fr

     This article is the transcript of the training capsule.

     End of the capsule transcript.
     """

  Scenario: Transcript generation skips when output already exists and is not blank
    Given a transcript plan for deck "demo" with language "fr" and strategy TEMPLATE
    And the plan contains slide segments:
      | title   | speakerNote               |
      | Intro   | Welcome.                  |
    And the output file already exists and contains "Existing transcript."
    When the transcript is generated
    Then the transcript should be "Existing transcript."
    And no enhancement should occur