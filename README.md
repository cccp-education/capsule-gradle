<!-- master source — other languages are translations of this file -->
# capsule-gradle — Consumer Guide

> Gradle plugin that turns a reveal.js deck into a synchronised video capsule — TTS voice-over, capture, subtitles, format conversion, audio post and a short-form/viral pipeline — without human post-production.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.10&color=green)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=License)](LICENSE)

- **Version**: `0.0.10` (Maven Central) · **Group**: `education.cccp` · **Plugin ID**: `education.cccp.capsule`
- **Requires**: Java 25+ · Gradle 9.8+
- **Docs**: [plugin internals](capsule-plugin/README.md) · [pipeline architecture](capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc)

🌐 Languages: **EN** | [中文](README.consommateurs/README.zh.md) | [हिन्दी](README.consommateurs/README.hi.md) | [Español](README.consommateurs/README.es.md) | [Français](README.consommateurs/README.fr.md) | [العربية](README.consommateurs/README.ar.md) | [বাংলা](README.consommateurs/README.bn.md) | [Português](README.consommateurs/README.pt.md) | [Русский](README.consommateurs/README.ru.md) | [اردو](README.consommateurs/README.ur.md)

---

## What it does

`capsule-gradle` consumes a reveal.js deck (produced by `slider-gradle`) and produces a
synchronised video capsule — one TTS voice-over per slide, video capture, subtitles,
format conversion, audio post-production, quality control and an optional vertical
short-form (viral) pipeline.

```
AsciiDoc source → slider-gradle → reveal.js deck + speaker script
                                        ↓
        capsule-gradle → TTS (Piper/espeak/Fish Audio) → audio injection → capture → WebM/MP4
                                        ↓                          ↓
                                Manim animation (optional)   FFmpeg 9:16 recrop (optional)
```

Part of the CCCP Education multi-plugin ecosystem (`groupId: education.cccp`).

## Requirements

- **Java** 25+ (Kotlin 2.4.10 toolchain)
- **Gradle** 9.8+
- **Piper** — local offline TTS (default engine)
- **espeak / espeak-ng** — TTS fallback (`espeak-ng` is used automatically when `espeak` is absent)
- **Fish Audio** — optional cloud TTS with a **cloned voice** (engine `fish`); the API key pool lives in a git-ignored `fish.yaml`
- **Chromium** — auto-downloaded by Playwright on first run
- **FFmpeg** — 9:16 recrop, subtitle burn-in, MP4 conversion, audio post
- **Manim** — optional, for animated mathematical slides
- **Node** — optional, for the Remotion capture/animation strategy

## Quick start

### 1. Apply the plugin

```gradle
plugins {
    id("education.cccp.capsule") version "0.0.10"
}
```

### 2. Configure

```gradle
capsule {
    outputDir.set("capsules")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    ttsEngine.set("espeak")   // "piper" (default), "espeak", or "fish" (Fish Audio cloud, cloned voice)
}
```

### 3. Generate the video

```bash
./gradlew generateCapsuleVideo
```

This runs the full pipeline: script validation → TTS synthesis → audio injection into the
deck HTML → headless capture → video output.

### Full pipeline with slider

```gradle
plugins {
    alias(libs.plugins.slider)
    alias(libs.plugins.capsule)
}

tasks.named("generateCapsuleVideo") {
    dependsOn("asciidoctorRevealJs", "asciidocCapsule")
}
```

```bash
./gradlew generateCapsuleVideo    # slider deck + script → capsule video
```

## Available tasks

| Task | Group | Description |
|------|-------|-------------|
| `generateCapsuleScript` | generate | Reads the `*-script.txt` produced by slider and validates the capsule script |
| `generateCapsule` | generate | Synthesises the TTS audio files (Piper/espeak/Fish Audio), one per slide |
| `generateCapsuleVideo` | generate | Injects the TTS audio into the deck HTML then captures the video |
| `generateCapsuleVideoAllLanguages` | generate | One localized capsule per target language (`LanguageCatalog`) |
| `extractSpeakerNotes` | capsule | Parses AsciiDoc speaker notes into the plain-text `*-script.txt` contract |
| `translateAndExtractSpeakerNotes` | capsule | Composite: `translateDeck` (slider) → `extractSpeakerNotes` |
| `translateAndGenerateCapsuleVideos` | capsule | Composite: `translateDeck` → `extractSpeakerNotes` → `generateCapsuleVideoAllLanguages` |
| `generateCapsuleContent` | generate | LLM pipeline (koog): propose-context → validate → speaker notes + TTS script |
| `generateCapsuleContentAndVideos` | capsule | Composite: `generateCapsuleContent` → `translateAndGenerateCapsuleVideos` |
| `collectCapsuleAugmentedContext` | collect | Collects the augmented context (governance + RAG + Docs + scenario + glossary) |
| `capsuleAiSmokeTest` | generate | Smoke-tests the codebase LLM bridge with a minimal prompt |
| `generateCapsuleTranscript` | generate | Produces an AsciiDoc transcript article from the speaker notes |
| `generateCapsuleChapters` | generate | Chapter markers + YouTube text chapters + description |
| `generateCapsulePodcast` | generate | Audio-only podcast (concatenated narration MP3) |
| `deployCapsule` | deploy | Recrops the capsule to vertical 9:16 (TikTok/Shorts) via FFmpeg |
| `distributeCapsuleVideo` | distribute | Copies the final videos to a versioned destination directory |
| `collectCapsuleContext` / `transformCapsuleContext` / `collectCapsuleRetrieve` | collect/transform | Export / parse the capsule context JSON (N3 engine contract) |
| `scaffoldCapsuleContext` | generate | Scaffolds a commented default `capsule-context.yml` |
| `validateCapsuleVideoDuration` | verification | Probes the video duration against the sum of TTS audio durations (opt-in) |
| `preflightCapsuleContent` | verification | Content pre-flight before the paid TTS (opt-in) |
| `renderQcBoards` | verification | Renders QC boards (beats + "said vs shown") (opt-in) |
| `verifyCapsuleVoice` | verification | Voice QC by ASR — fidelity, breath, take choice (opt-in) |
| `selectBestVoiceTake` | verification | Selects the best take among a slide's candidates (opt-in) |
| `generateViralCampaign` | generate | Storyboard → short-form campaign (YouTube/TikTok) (opt-in) |
| `renderViralVariants` / `renderViralThumbnails` / `renderViralSubtitles` | generate | Render the viral variants, thumbnails, karaoke subtitles (opt-in) |
| `validateViralCampaign` | verification | Checks the manifest against the produced files (opt-in) |
| `normalizeViralStoryboard` | generate | Fills missing storyboard transitions (authoring helper) |

> The optional (*opt-in*) capabilities are **disabled by default** (`false`) and never
> block the main pipeline.

## Configuration

Every value is resolved from **4 sources** (priority **CLI > YAML > gradle.properties > ENV**):

```bash
./gradlew scaffoldCapsuleContext          # generate a commented capsule-context.yml
./gradlew generateCapsuleVideo -Pcapsule.tts.engine=espeak -Pcapsule.manim.quality=h
```

Representative DSL (see the [plugin internals guide](capsule-plugin/README.md) for the full
18-section `CapsuleConfig`):

```gradle
capsule {
    // Input
    outputDir.set("capsule")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")

    // TTS
    ttsEngine.set("piper")                 // "piper" (default), "espeak", or "fish"
    ttsVoice.set("fr_FR-siwis-medium")
    piperExecutablePath.set("piper")
    ttsFallbackEnabled.set(true)
    espeakVoice.set("fr")
    espeakSpeed.set(150)
    ttsLanguage.set("fr")                  // 22 languages (LanguageCatalog)

    // TTS — Fish Audio (engine "fish"; the API key pool is in the git-ignored fish.yaml)
    ttsFishVoice.set("<cloned voice reference_id>")
    ttsFishModel.set("speech-1.6")
    ttsFishFormat.set("wav")
    ttsFishTemperature.set(0.85)
    ttsFishTopP.set(0.88)
    ttsFishEndpoint.set("https://api.fish.audio/v1/tts")
    fishAudioConfigPath.set("fish.yaml")   // the key file (never committed)

    // Capture / output
    viewportWidth.set(1408)
    viewportHeight.set(792)
    captureStrategy.set("playwright")      // "playwright" | "screenshot" | "remotion"
    outputFormat.set("webm")               // "webm" (default) | "mp4" | "both"
    outputBt709.set(false)                 // tag the MP4 as BT.709 (opt-in)

    // Subtitles
    subtitleEnabled.set(false)
    subtitleFormat.set("srt")              // "srt" or "vtt"
    subtitleBurnIn.set(false)

    // Distribution
    ffmpegExecutablePath.set("ffmpeg")
    distribOutputWidth.set(1080)
    distribOutputHeight.set(1920)

    // Manim (optional)
    manimExecutablePath.set("manim")
    manimQuality.set("l")                  // l, m, h, p, k
}
```

## Multi-language pipeline

One source deck → one localized capsule per target language. The pipeline consumes the
translated decks produced by `slider.translateDeck` (**22 languages**, `LanguageCatalog`
from the N0 `i18n-contracts` BOM) and resolves the per-language TTS voice via the pure
`capsule.multilang` domain (`MultiLanguageResolver` + `VoiceMapping`).

```
source deck (AsciiDoc)
        ↓ slider.translateDeck (LanguageCatalog, 22 languages)
<deckName>_<lang>-deck.html (one per language)
        ↓ capsule.extractSpeakerNotes
<deckName>_<lang>-script.txt (one per language)
        ↓ capsule.generateCapsuleVideoAllLanguages
<deckName>_<lang>.webm (one localized capsule per language)
```

File naming convention (boundary contract, aligned with slider):

- translated deck: `<deckName>_<lang>-deck.html`
- speaker script:  `<deckName>_<lang>-script.txt`
- localized video: `<deckName>_<lang>.webm`

```bash
./gradlew generateCapsuleVideoAllLanguages   # one video per language with a deck+script pair
./gradlew translateAndGenerateCapsuleVideos  # full composite: translate → extract → videos
```

A language without a matching deck + script pair is silently skipped, and the **Economy of
Ink** rule applies: a language whose video already exists and probes valid is never re-rendered.

## Optional features

- **Output format** — `outputFormat = "mp4"` (or `"both"`) produces an MP4/H.264 in addition
  to the native WebM; `outputBt709 = true` tags it as BT.709 (broadcast colorimetry).
- **Audio post-production** — background music, EBU R128 loudness (2-pass measured) and
  sidechain ducking (`audioPost { … }`).
- **Subtitles** — SRT/VTT generation and burn-in (`subtitleEnabled`, `subtitleBurnIn`).
- **Quality control (opt-in)** — content pre-flight (`preflightCapsuleContent`), video
  integrity by decoding, deterministic timeline, QC boards (`renderQcBoards`), and voice
  QC by ASR (`verifyCapsuleVoice`, `selectBestVoiceTake`).
- **Transcript** — reverse pipeline: an AsciiDoc article from the speaker notes
  (`TEMPLATE` or `LLM`).
- **Podcast** — audio-only MP3 of the narration.
- **Preview** — dry-run (`previewOnly = true`): render the slide PNGs only, skip the mux.
- **Viral (opt-in)** — a validatable storyboard → vertical 9:16 short-form campaign with
  hooks/CTA anchored on the augmented context, thumbnails, karaoke subtitles and a
  `CampaignBundle`.

### Slide rhythm (Remotion capture)

With `captureStrategy = "remotion"`, the deck is animated rather than filmed: each block of
a slide lands on a reading rhythm instead of the whole slide appearing at once. Default beats
(seconds from the start of the slide): `.kicker` 0.0 · `h1`/`h2` 0.15 · `.rule` 0.5 ·
`.lede` 1.0 · `.stat` 4.5 · `.statlabel` 5.0. `.brand`, `.num` and `.grid` are chrome
(present from the first frame). Any block can set its own beat with `data-at`:

```html
<p class="stat" data-at="8.5">542 076 km</p>
```

## Build & test (contributors)

```bash
./gradlew build                    # full build
./gradlew check                    # unit + functional + Cucumber
./gradlew test                     # JUnit5 unit only
./gradlew cucumberTest -PrunCucumber
./gradlew functionalTest           # Gradle TestKit
./gradlew publishToMavenLocal      # publish locally
```

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Playwright Chromium not found | run once to auto-download, or set `chromiumExecutablePath` |
| Piper TTS produces no audio | check `piperExecutablePath`, ensure the voice model is installed |
| Fish Audio falls back to noop | set `ttsEngine` to `fish`, provide a cloned voice (`ttsFishVoice`) and a key pool in the git-ignored `fish.yaml` |
| `deployCapsule` fails | verify FFmpeg is on `PATH` (`ffmpeg -version`) |
| Manim slides not rendered | set `manimExecutablePath` to `noop` to test without Manim |
| Video capture timeout | increase `playwrightTimeout` (default 120000 ms) |
| Out of memory | `export GRADLE_OPTS="-Xmx2g"` |

## Documentation

- [capsule-plugin/README.md](capsule-plugin/README.md) — plugin internals (developer & contributor guide)
- [capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc](capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) — pipeline architecture (PlantUML)

## License

Apache License 2.0 — see [LICENSE](LICENSE).

---

_Part of the CCCP Education ecosystem — `groupId: education.cccp`._
