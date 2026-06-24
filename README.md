<!-- master source — other languages are translations of this file -->
# capsule-gradle — Consumer Guide

> Gradle plugin for automated pedagogical video capsule generation from reveal.js decks.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.1-SNAPSHOT&color=orange)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=License)](../LICENSE)

- **Version**: `0.0.1-SNAPSHOT` (non publié) · **Group**: `education.cccp` · **Plugin ID**: `education.cccp.capsule`
- **Build**: `./gradlew build` · **Tests**: `./gradlew check` (195 unit + 22 Cucumber PASS)
- **Coverage**: Kover XML + HTML reports wired into `check`

🌐 Languages: **EN** | [中文](README.consommateurs/README.zh.md) | [हिन्दी](README.consommateurs/README.hi.md) | [Español](README.consommateurs/README.es.md) | [Français](README.consommateurs/README.fr.md) | [العربية](README.consommateurs/README.ar.md) | [বাংলা](README.consommateurs/README.bn.md) | [Português](README.consommateurs/README.pt.md) | [Русский](README.consommateurs/README.ru.md) | [اردو](README.consommateurs/README.ur.md)

---

## What it does

`capsule-gradle` transforms a reveal.js deck (produced by `slider-gradle`) into a
pedagogical video capsule with synchronized TTS narration — no post-production required.

The pipeline orchestrates four external tools:

```
AsciiDoc source → slider-gradle → reveal.js deck + capsule script
                                      ↓
              capsule-gradle → TTS (Piper/espeak) → audio injection → Playwright capture → WebM video
                                      ↓                              ↓
                              Manim animation (optional)      FFmpeg distrib (9:16)
```

Part of the CCCP Education multi-plugin ecosystem (`groupId: education.cccp`).

## Quick Start

### 1. Apply the plugin

```gradle
plugins {
    id("education.cccp.capsule") version "0.0.1-SNAPSHOT"
}
```

### 2. Configure the capsule

```gradle
capsule {
    outputDir.set("capsules")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    ttsEngine.set("espeak")
}
```

### 3. Generate the video

```bash
./gradlew generateCapsuleVideo
```

This runs the full pipeline: script validation → TTS synthesis → audio injection into
deck HTML → Playwright headless capture → WebM output.

### Full pipeline with slider

The root `build.gradle.kts` wires slider and capsule together:

```gradle
plugins {
    alias(libs.plugins.slider)
    alias(libs.plugins.capsule)
}

tasks.named("capsulevideo") {
    dependsOn("asciidoctorRevealJs", "asciidocCapsule")
}
```

```bash
./gradlew capsulevideo    # slider deck + script → capsule video
```

## Available tasks

| Task | Group | Description |
|------|-------|-------------|
| `generateCapsuleScript`   | generate  | Reads `*-script.txt` from slider and validates the capsule script |
| `generateCapsule`          | generate  | Generates TTS audio files from capsule scripts (Piper/espeak) |
| `generateCapsuleVideo`     | generate  | Injects TTS audio into deck HTML then captures video via Playwright |
| `deployCapsule`            | deploy    | Recrops capsules to vertical 9:16 (TikTok/Shorts) via FFmpeg |
| `collectCapsuleContext`    | collect   | Exports capsule context (video paths + metadata) as JSON for N3 engine |
| `transformCapsuleContext`  | transform | Parses `capsule-context.json` and returns a list of decks |
| `collectCapsuleRetrieve`   | collect   | Retrieves capsule decks from context JSON (N3 engine contract) |
| `scaffoldCapsuleContext`   | generate  | Scaffolds a default `capsule-context.yml` config file with comments |

## Extension DSL

```gradle
capsule {
    // Input
    outputDir.set("capsule")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    chromiumExecutablePath.set("")

    // TTS
    ttsEngine.set("piper")                    // "piper" or "espeak"
    ttsVoice.set("fr_FR-siwis-medium")
    piperExecutablePath.set("piper")
    ttsFallbackEnabled.set(true)
    espeakVoice.set("fr")
    espeakSpeed.set(150)
    ttsLanguage.set("fr")                     // fr, en, es, de

    // Capture
    viewportWidth.set(1408)
    viewportHeight.set(792)
    playwrightTimeout.set(120_000.0)
    slideDurationSeconds.set(5.0)
    parallelCaptureEnabled.set(false)
    parallelCaptureThreads.set(4)

    // Subtitles
    subtitleEnabled.set(false)
    subtitleFormat.set("srt")                 // "srt" or "vtt"
    subtitleBurnIn.set(false)
    subtitleBurnInFontSize.set(24)
    subtitleBurnInFontColor.set("&H00FFFFFF")
    subtitleBurnInOutlineColor.set("&H00000000")
    subtitleBurnInPosition.set("bottom")      // "bottom" or "top"

    // Distribution
    ffmpegExecutablePath.set("ffmpeg")
    distribOutputWidth.set(1080)
    distribOutputHeight.set(1920)

    // Manim animation
    manimExecutablePath.set("manim")
    manimQuality.set("l")                     // l, m, h, p, k
    manimScriptsDir.set("src/manim")
    manimOutputDir.set("build/capsule/manim")
    manimParallelRender.set(false)
    manimParallelRenderThreads.set(4)
}
```

### Multi-source configuration

Configuration is resolved from 4 sources (priority: CLI > YAML > gradle.properties > ENV):

```bash
./gradlew scaffoldCapsuleContext          # generate capsule-context.yml
./gradlew generateCapsuleVideo -Pcapsule.tts.engine=espeak -Pcapsule.manim.quality=h
```

## Prerequisites

- **Java** 24+ (Kotlin 2.3.20 toolchain)
- **Gradle** 9.5+
- **Piper** (TTS, local offline) — default engine
- **espeak** (TTS fallback)
- **Chromium** (auto-downloaded by Playwright on first run)
- **FFmpeg** (for `deployCapsule` 9:16 recrop and subtitle burn-in)
- **Manim** (optional, for mathematical animation slides)

## Build & test

```bash
./gradlew build                    # full build
./gradlew check                    # unit + functional + Cucumber tests
./gradlew test                     # JUnit5 unit tests (excludes @integration)
./gradlew cucumberTest             # Cucumber BDD tests
./gradlew functionalTest           # Gradle TestKit functional tests
./gradlew publishToMavenLocal      # publish locally
```

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| Playwright Chromium not found     | run once to auto-download, or set `chromiumExecutablePath` |
| Piper TTS produces no audio       | check `piperExecutablePath`, ensure voice model is installed |
| `deployCapsule` fails              | verify FFmpeg is on PATH (`ffmpeg -version`) |
| Manim slides not rendered          | set `manimExecutablePath` to `noop` for testing without Manim |
| Video capture timeout              | increase `playwrightTimeout` (default 120000ms) |
| Out of memory                      | `export GRADLE_OPTS="-Xmx2g"` |

See [CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) for pipeline details.

## License

Apache License 2.0 — see [LICENSE](../LICENSE).

---

_Part of the CCCP Education ecosystem — `groupId: education.cccp`._