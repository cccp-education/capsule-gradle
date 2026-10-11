@qc-boards
Feature: Visual QC boards (PRT-08)

  As a capsule-gradle producer
  I want boards showing each slide's final state and what it said
  So that I can review a capsule before shipping it

  Scenario: The beats board lists every slide
    Given a QC deck slide 1 titled "Intro" saying "bonjour"
    And a QC deck slide 2 titled "Plan" saying "le programme"
    When the beats board is planned
    Then the QC board has 2 cells
    And the QC board title is "Beats — demo"

  Scenario: The rendered board is a readable image
    Given a QC deck slide 1 titled "Intro" saying "bonjour"
    When the beats board is rendered
    Then the QC board image is readable
