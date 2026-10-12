@engine-adapter
Feature: Reveal.js video engine adapter
  The reveal.js deck HTML concerns and the capture orchestration live in the
  capsule.adapter.revealjs package, behind the neutral VideoEngine port; the
  video task delegates to them without leaking reveal vocabulary into the kernel.

  Scenario: a single slide is extracted as a standalone document
    Given a reveal deck with 3 slides
    When slide 2 of the deck is extracted as a standalone document
    Then the standalone document contains "<h1>Two</h1>"
    And the standalone document does not contain "<h1>One</h1>"

  Scenario: audio is wired into the matching section
    Given a reveal deck with 3 slides
    When the audio file "/audio/slide-02.mp3" is wired to section 2
    Then the deck carries data-audio "file:///audio/slide-02.mp3" on section 2

  Scenario: the engine adapter renders a whole source with a fake engine
    Given a reveal engine adapter backed by a fake capture writing "capsule.webm"
    And a render request for "demo" in non-parallel mode
    When the engine adapter renders the request
    Then the produced video is "capsule.webm"
    And no segment failed

  Scenario: a failing segment is reported, never fatal
    Given a reveal engine adapter backed by a fake capture failing on "slide-02"
    And a render request for "demo" in parallel mode with 2 segments
    When the engine adapter renders the request
    Then the failed segments are "2"
