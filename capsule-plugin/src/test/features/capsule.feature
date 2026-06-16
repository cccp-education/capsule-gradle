Feature: Capsule video generation from a reveal.js deck
  As an instructor
  I want to generate a video capsule from a reveal.js deck with synchronized TTS audio
  So that I can produce WebM pedagogical capsules without post-production

  Background:
    Given a Gradle project with the capsule plugin applied

  Scenario: Generate a WebM capsule from a deck with capsule script
    Given a reveal.js deck "mon-cours-deck.html" with 2 slides and data-capsule-slide attributes
    And a capsule script "mon-cours-script.txt" with 2 slide segments
    When I run the task "generateCapsuleVideo" with NoOp capture
    Then a video file "mon-cours.webm" is generated
    And the video file is not empty

  Scenario: NoOp fallback when Chromium is unavailable
    Given a reveal.js deck "test-deck.html" with 1 slides and data-capsule-slide attributes
    And a capsule script "test-script.txt" with 1 slide segments
    When I run the task "generateCapsuleVideo" with NoOp capture
    Then the task completes without error
    And a placeholder video is generated

  Scenario: Sequential fallback for deck without data-capsule-slide attributes
    Given a reveal.js deck "seq-deck.html" with 3 slides without data-capsule-slide attributes
    And a capsule script "seq-script.txt" with 3 sequentially ordered slide segments
    When I run the task "generateCapsuleVideo" with NoOp capture
    Then a video file "seq.webm" is generated
    And the injected deck HTML contains audio attributes for all slides

  Scenario: Multi-deck build produces separate videos
    Given a reveal.js deck "deck-a-deck.html" with 1 slides and data-capsule-slide attributes
    And a reveal.js deck "deck-b-deck.html" with 1 slides and data-capsule-slide attributes
    And a capsule script "deck-a-script.txt" with 1 slide segments
    And a capsule script "deck-b-script.txt" with 1 slide segments
    When I run the task "generateCapsuleVideo" with NoOp capture
    Then a video file "deck-a.webm" is generated
    And a video file "deck-b.webm" is generated

  Scenario: Audio injection into deck HTML
    Given a reveal.js deck "audio-deck.html" with 2 slides and data-capsule-slide attributes
    And a capsule script "audio-script.txt" with 2 slide segments
    When I run the task "generateCapsuleVideo" with NoOp capture
    Then the injected deck HTML contains "data-audio" attributes
    And the injected deck contains the "CAPSULE-GRADLE" autoplay script

  Scenario: Composite context JSON is parsed for engine N3 contract
    Given a reveal.js deck "ctx-deck.html" with 2 slides and data-capsule-slide attributes
    And a capsule script "ctx-script.txt" with 2 slide segments
    When I run the task "generateCapsuleVideo" with NoOp capture
    And I run the task "deployCapsule"
    And I run the task "collectCapsuleContext"
    And I run the task "transformCapsuleContext"
    Then the parsed output contains "deckName"
    And the parsed output contains "originalVideo"
    And the parsed output is a valid JSON array

  @integration
  Scenario: Audio constraint — real TTS must produce binary MP3 not text
    Given a Gradle project with the capsule plugin configured for espeak TTS
    And a reveal.js deck "audio-real-deck.html" with 1 slides and data-capsule-slide attributes
    And a capsule script "audio-real-script.txt" with 1 slide segments
    When I run the task "generateCapsule" with espeak TTS
    Then the generated MP3 files must be binary audio not text placeholder

  @integration
  Scenario: Output constraint — capsule video must be written to build/capsules/ directory
    Given a Gradle project with the capsule plugin configured for noop TTS
    And a reveal.js deck "out-deck.html" with 1 slides and data-capsule-slide attributes
    And a capsule script "out-script.txt" with 1 slide segments
    When I run the task "generateCapsuleVideo" with NoOp capture
    Then the video file "out.webm" exists in the build "capsules" directory

  @integration
  Scenario: WebM validity constraint — generated video must have EBML header
    Given a Gradle project with the capsule plugin configured for noop TTS
    And a reveal.js deck "webm-deck.html" with 1 slides and data-capsule-slide attributes
    And a capsule script "webm-script.txt" with 1 slide segments
    When I run the task "generateCapsuleVideo" with NoOp capture
    Then the video file "webm.webm" has a valid WebM EBML header

  @manim
  Scenario: Manim slide type is detected in capsule script and triggers ManimEngine render
    Given a reveal.js deck "manim-deck.html" with 1 slides and data-capsule-slide attributes
    And a capsule script "manim-course-script.txt" with 1 manim slide segments
    When I run the task "generateCapsuleVideo" with NoOp capture
    Then the capsule script contains a slide with type MANIM
    And the ManimEngine render is invoked for the manim slide

  @manim
  Scenario: Manim slide video is muxed with TTS audio via ManimVideoMixer
    Given a reveal.js deck "mux-deck.html" with 2 slides and data-capsule-slide attributes
    And a capsule script "mux-course-script.txt" with 1 manim slide segments
    When I run the task "generateCapsuleVideo" with NoOp capture
    Then the ManimEngine render produces a muxed MP4 with TTS audio for the manim slide

  @manim
  Scenario: Manim slide HTML is replaced by video embed via ManimSlideReplacer
    Given a reveal.js deck "replace-deck.html" with 2 slides and data-capsule-slide attributes
    And a capsule script "replace-course-script.txt" with 1 manim slide segments
    When I run the task "generateCapsuleVideo" with NoOp capture
    Then the ManimSlideReplacer replaces the manim slide section with a video embed

  @integration @manim
  Scenario: Manim E2E pipeline — NoOp render, mux, and slide replacement produce valid output
    Given a Gradle project with the capsule plugin configured for noop Manim
    And a reveal.js deck "pipeline-deck.html" with 2 slides and data-capsule-slide attributes
    And a capsule script "pipeline-course-script.txt" with 1 manim slide segments
    And a Manim script "Scene1.py" in the manim scripts directory
    When I run the task "generateCapsuleVideo" with NoOp capture
    Then the ManimEngine produces a placeholder MP4 for the manim slide
    And the ManimVideoMixer produces a muxed MP4 for the manim slide
    And the ManimSlideReplacer replaces the manim slide section with a video embed
    And the replaced deck is saved in the build output directory

  @config
  Scenario: Scaffold creates capsule-context yml with default configuration
    Given a Gradle project with the capsule plugin applied
    When I run the task "scaffoldCapsuleContext"
    Then a file named "capsule-context.yml" exists in the project directory
    And the scaffold file contains all 5 configuration sections

  @config
  Scenario: Scaffold does not overwrite existing capsule-context yml
    Given a Gradle project with the capsule plugin applied and an existing capsule-context.yml
    When I run the task "scaffoldCapsuleContext"
    Then the existing capsule-context.yml is preserved unchanged

  @config
  Scenario: YAML configuration overrides gradle.properties defaults
    Given a Gradle project with the capsule plugin applied and a capsule-context.yml setting espeak TTS
    When I run the task "generateCapsuleScript"
    Then the resolved TTS engine is "espeak"

  @config @manim
  Scenario: Manim YAML configuration overrides manim defaults for quality and scriptsDir
    Given a Gradle project with the capsule plugin applied and a capsule-context.yml setting manim quality to "h" and scriptsDir to "custom/manim"
    When I run the task "generateCapsuleScript"
    Then the resolved manim quality is "h"
    And the resolved manim scriptsDir is "custom/manim"

  @config @manim
  Scenario: Manim CLI -P params override YAML manim configuration
    Given a Gradle project with the capsule plugin applied and a capsule-context.yml setting manim quality to "m"
    When I run the task "generateCapsuleScript" with CLI param "-Pcapsule.manim.quality=k"
    Then the resolved manim quality is "k"

  @config @manim
  Scenario: Manim default configuration has low quality and src/manim scriptsDir
    Given a Gradle project with the capsule plugin applied
    When I run the task "generateCapsuleScript"
    Then the resolved manim quality is "l"
    And the resolved manim scriptsDir is "src/manim"

  @config @manim
  Scenario: Manim outputDir is resolved from YAML configuration
    Given a Gradle project with the capsule plugin applied and a capsule-context.yml setting manim outputDir to "custom/output"
    When I run the task "generateCapsuleScript"
    Then the resolved manim outputDir is "custom/output"

  @config @manim
  Scenario: Manim configuration section contains all 5 fields after scaffold
    Given a Gradle project with the capsule plugin applied
    When I run the task "scaffoldCapsuleContext"
    Then the scaffold file contains the manim section with executablePath, quality, scriptsDir, and outputDir

  @manim @parallel
  Scenario: Manim parallel render — multiple manim slides are rendered in parallel
    Given a reveal.js deck "parallel-deck.html" with 3 slides and data-capsule-slide attributes
    And a capsule script "parallel-course-script.txt" with 2 manim slide segments
    When I run the task "generateCapsuleVideo" with NoOp capture
    Then the ManimEngine renders all manim slides for the deck
    And each manim slide produces a rendered video file in the output directory

  @tts @config
  Scenario: TTS language configuration defaults to French
    Given a Gradle project with the capsule plugin applied
    When I run the task "generateCapsuleScript"
    Then the resolved TTS language is "fr"

  @tts @config
  Scenario: TTS language can be set to English via DSL
    Given a Gradle project with the capsule plugin configured for English TTS
    When I run the task "generateCapsuleScript"
    Then the resolved TTS language is "en"

  @tts @config
  Scenario: TTS language can be set to Spanish via DSL
    Given a Gradle project with the capsule plugin configured for Spanish TTS
    When I run the task "generateCapsuleScript"
    Then the resolved TTS language is "es"

  @tts @config
  Scenario: TTS language can be set to German via DSL
    Given a Gradle project with the capsule plugin configured for German TTS
    When I run the task "generateCapsuleScript"
    Then the resolved TTS language is "de"

  @tts @config
  Scenario: TTS language overrides voice mapping for Piper engine
    Given a Gradle project with the capsule plugin configured for English Piper TTS
    When I run the task "generateCapsuleScript"
    Then the resolved TTS engine is "piper"
    And the resolved TTS language is "en"

  @tts @config
  Scenario: TTS language overrides voice mapping for espeak engine
    Given a Gradle project with the capsule plugin configured for German espeak TTS
    When I run the task "generateCapsuleScript"
    Then the resolved TTS engine is "espeak"
    And the resolved TTS language is "de"

  @subtitles @config
  Scenario: Subtitle generation is disabled by default
    Given a Gradle project with the capsule plugin applied
    When I run the task "generateCapsuleScript"
    Then the resolved subtitle enabled is "false"
    And the resolved subtitle format is "srt"

  @subtitles @config
  Scenario: Subtitle generation can be enabled via DSL with SRT format
    Given a Gradle project with the capsule plugin configured for SRT subtitles
    When I run the task "generateCapsuleScript"
    Then the resolved subtitle enabled is "true"
    And the resolved subtitle format is "srt"

  @subtitles @config
  Scenario: Subtitle generation can be enabled via DSL with VTT format
    Given a Gradle project with the capsule plugin configured for VTT subtitles
    When I run the task "generateCapsuleScript"
    Then the resolved subtitle enabled is "true"
    And the resolved subtitle format is "vtt"

  @subtitles
  Scenario: SRT subtitle file is generated alongside the video
    Given a Gradle project with the capsule plugin configured for SRT subtitles
    And a reveal.js deck "sub-deck.html" with 2 slides and data-capsule-slide attributes
    And a capsule script "sub-script.txt" with 2 slide segments
    When I run the task "generateCapsuleVideo" with NoOp capture
    Then a subtitle file "sub.srt" is generated in the capsule output directory
    And the subtitle file contains valid SRT format with 2 cues

  @subtitles
  Scenario: VTT subtitle file is generated alongside the video
    Given a Gradle project with the capsule plugin configured for VTT subtitles
    And a reveal.js deck "vtt-deck.html" with 2 slides and data-capsule-slide attributes
    And a capsule script "vtt-script.txt" with 2 slide segments
    When I run the task "generateCapsuleVideo" with NoOp capture
    Then a subtitle file "vtt.vtt" is generated in the capsule output directory
    And the subtitle file contains valid VTT format with WEBVTT header

  @subtitles
  Scenario: Subtitle track element is injected into deck HTML
    Given a Gradle project with the capsule plugin configured for SRT subtitles
    And a reveal.js deck "track-deck.html" with 2 slides and data-capsule-slide attributes
    And a capsule script "track-script.txt" with 2 slide segments
    When I run the task "generateCapsuleVideo" with NoOp capture
    Then the injected deck HTML contains a track element for subtitles

  @subtitles @burnin @config
  Scenario: Subtitle burn-in is disabled by default
    Given a Gradle project with the capsule plugin applied
    When I run the task "generateCapsuleScript"
    Then the resolved subtitle burn-in is "false"

  @subtitles @burnin @config
  Scenario: Subtitle burn-in can be enabled via DSL
    Given a Gradle project with the capsule plugin configured for SRT subtitles with burn-in
    When I run the task "generateCapsuleScript"
    Then the resolved subtitle burn-in is "true"

  @subtitles @burnin
  Scenario: Subtitle burn-in is applied to the final video when enabled
    Given a Gradle project with the capsule plugin configured for SRT subtitles with burn-in
    And a reveal.js deck "burnin-deck.html" with 2 slides and data-capsule-slide attributes
    And a capsule script "burnin-script.txt" with 2 slide segments
    When I run the task "generateCapsuleVideo" with NoOp capture
    Then the subtitle burn-in service is invoked for the final video

  @subtitles @burnin @integration
  Scenario: Burn-in E2E pipeline — full pipeline with real ffmpeg subtitle burn-in produces valid WebM
    Given a Gradle project with the capsule plugin configured for SRT subtitles with burn-in and real ffmpeg
    And a reveal.js deck "burnin-e2e-deck.html" with 2 slides and data-capsule-slide attributes
    And a capsule script "burnin-e2e-script.txt" with 2 slide segments
    When I run the task "generateCapsuleVideo" with NoOp capture
    Then the subtitle burn-in service is invoked for the final video
    And a subtitle file "burnin-e2e.srt" is generated in the capsule output directory
    And a video file "burnin-e2e.webm" is generated
    And the video file "burnin-e2e.webm" has a valid WebM EBML header
    And the burn-in operation completed successfully on the final video

  @subtitles @burnin @style @config
  Scenario: Subtitle burn-in style defaults are resolved correctly
    Given a Gradle project with the capsule plugin applied
    When I run the task "generateCapsuleScript"
    Then the resolved subtitle burn-in is "false"

  @subtitles @burnin @style
  Scenario: Subtitle burn-in with custom style is applied to the final video
    Given a Gradle project with the capsule plugin configured for SRT subtitles with burn-in and custom style
    And a reveal.js deck "style-deck.html" with 2 slides and data-capsule-slide attributes
    And a capsule script "style-script.txt" with 2 slide segments
    When I run the task "generateCapsuleVideo" with NoOp capture
    Then the subtitle burn-in service is invoked for the final video
    And the resolved subtitle burn-in style has fontSize 36
    And the resolved subtitle burn-in style has fontColor "&H000000FF"
    And the resolved subtitle burn-in style has position "top"
