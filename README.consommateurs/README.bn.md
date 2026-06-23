<!-- translated from README.md rev 0.0.1-SNAPSHOT -->
# capsule-gradle — ভোক্তা গাইড

> Gradle প্লাগইন যা reveal.js ডেক থেকে স্বয়ংক্রিয় শিক্ষামূলক ভিডিও ক্যাপসুল তৈরি করে।

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.1-SNAPSHOT&color=orange)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=License)](../LICENSE)

- **সংস্করণ**: `0.0.1-SNAPSHOT` (প্রকাশিত নয়) · **গ্রুপ**: `education.cccp` · **প্লাগইন আইডি**: `education.cccp.capsule`
- **বিল্ড**: `./gradlew build` · **পরীক্ষা**: `./gradlew check` (195 ইউনিট + 22 Cucumber পাস)
- **কভারেজ**: Kover XML + HTML রিপোর্ট `check`-এ সংযুক্ত

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | **বাংলা** | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## এটি কী করে

`capsule-gradle` একটি reveal.js ডেক (`slider-gradle` দ্বারা নির্মিত) কে সিঙ্ক্রোনাইজড TTS ন্যারেশনসহ একটি শিক্ষামূলক ভিডিও ক্যাপসুলে রূপান্তর করে — কোনো পোস্ট-প্রোডাকশন ছাড়াই।

পাইপলাইনটি চারটি বাহ্যিক টুল অর্কেস্ট্রেট করে:

```
AsciiDoc source → slider-gradle → reveal.js deck + capsule script
                                      ↓
              capsule-gradle → TTS (Piper/espeak) → audio injection → Playwright capture → WebM video
                                      ↓                              ↓
                              Manim animation (optional)      FFmpeg distrib (9:16)
```

CCCP Education মাল্টি-প্লাগইন ইকোসিস্টেমের অংশ (`groupId: education.cccp`)।

## দ্রুত শুরু

### 1. প্লাগইন প্রয়োগ করুন

```gradle
plugins {
    id("education.cccp.capsule") version "0.0.1-SNAPSHOT"
}
```

### 2. ক্যাপসুল কনফিগার করুন

```gradle
capsule {
    outputDir.set("capsules")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    ttsEngine.set("espeak")
}
```

### 3. ভিডিও তৈরি করুন

```bash
./gradlew generateCapsuleVideo
```

এটি সম্পূর্ণ পাইপলাইন চালায়: স্ক্রিপ্ট যাচাই → TTS সংশ্লেষণ → ডেক HTML-এ অডিও ইনজেকশন → Playwright হেডলেস ক্যাপচার → WebM আউটপুট।

### slider সহ সম্পূর্ণ পাইপলাইন

রুট `build.gradle.kts` slider এবং capsule একসাথে যুক্ত করে:

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
./gradlew capsulevideo    # slider ডেক + স্ক্রিপ্ট → ক্যাপসুল ভিডিও
```

## উপলব্ধ টাস্ক

| টাস্ক | গ্রুপ | বিবরণ |
|------|-------|-------------|
| `generateCapsuleScript`   | generate  | slider থেকে `*-script.txt` পড়ে এবং ক্যাপসুল স্ক্রিপ্ট যাচাই করে |
| `generateCapsule`          | generate  | ক্যাপসুল স্ক্রিপ্ট থেকে TTS অডিও ফাইল তৈরি করে (Piper/espeak) |
| `generateCapsuleVideo`     | generate  | ডেক HTML-এ TTS অডিও ইনজেক্ট করে তারপর Playwright দিয়ে ভিডিও ক্যাপচার করে |
| `deployCapsule`            | deploy    | FFmpeg দিয়ে ক্যাপসুলগুলিকে উল্লম্ব 9:16 (TikTok/Shorts) হিসেবে রিক্রপ করে |
| `collectCapsuleContext`    | collect   | ক্যাপসুল প্রসঙ্গ (ভিডিও পথ + মেটাডেটা) JSON হিসেবে রপ্তানি করে, N3 ইঞ্জিনের জন্য |
| `transformCapsuleContext`  | transform | `capsule-context.json` পার্স করে এবং ডেকের তালিকা ফেরায় |
| `collectCapsuleRetrieve`   | collect   | প্রসঙ্গ JSON থেকে ক্যাপসুল ডেক পুনরুদ্ধার করে (N3 ইঞ্জিন চুক্তি) |
| `scaffoldCapsuleContext`   | generate  | মন্তব্যসহ একটি ডিফল্ট `capsule-context.yml` কনফিগ ফাইল তৈরি করে |

## এক্সটেনশন DSL

```gradle
capsule {
    // ইনপুট
    outputDir.set("capsule")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    chromiumExecutablePath.set("")

    // TTS
    ttsEngine.set("piper")                    // "piper" বা "espeak"
    ttsVoice.set("fr_FR-siwis-medium")
    piperExecutablePath.set("piper")
    ttsFallbackEnabled.set(true)
    espeakVoice.set("fr")
    espeakSpeed.set(150)
    ttsLanguage.set("fr")                     // fr, en, es, de

    // ক্যাপচার
    viewportWidth.set(1408)
    viewportHeight.set(792)
    playwrightTimeout.set(120_000.0)
    slideDurationSeconds.set(5.0)
    parallelCaptureEnabled.set(false)
    parallelCaptureThreads.set(4)

    // সাবটাইটেল
    subtitleEnabled.set(false)
    subtitleFormat.set("srt")                 // "srt" বা "vtt"
    subtitleBurnIn.set(false)
    subtitleBurnInFontSize.set(24)
    subtitleBurnInFontColor.set("&H00FFFFFF")
    subtitleBurnInOutlineColor.set("&H00000000")
    subtitleBurnInPosition.set("bottom")      // "bottom" বা "top"

    // বিতরণ
    ffmpegExecutablePath.set("ffmpeg")
    distribOutputWidth.set(1080)
    distribOutputHeight.set(1920)

    // Manim অ্যানিমেশন
    manimExecutablePath.set("manim")
    manimQuality.set("l")                     // l, m, h, p, k
    manimScriptsDir.set("src/manim")
    manimOutputDir.set("build/capsule/manim")
    manimParallelRender.set(false)
    manimParallelRenderThreads.set(4)
}
```

### মাল্টি-সোর্স কনফিগারেশন

কনফিগারেশন 4টি উৎস থেকে সমাধান করা হয় (অগ্রাধিকার: CLI > YAML > gradle.properties > ENV):

```bash
./gradlew scaffoldCapsuleContext          # capsule-context.yml তৈরি করুন
./gradlew generateCapsuleVideo -Pcapsule.tts.engine=espeak -Pcapsule.manim.quality=h
```

## পূর্বশর্ত

- **Java** 24+ (Kotlin 2.3.20 টুলচেইন)
- **Gradle** 9.5+
- **Piper** (TTS, স্থানীয় অফলাইন) — ডিফল্ট ইঞ্জিন
- **espeak** (TTS ফলব্যাক)
- **Chromium** (প্রথম রানে Playwright দ্বারা স্বয়ংক্রিয়ভাবে ডাউনলোড হয়)
- **FFmpeg** (`deployCapsule` 9:16 রিক্রপ এবং সাবটাইটেল বার্ন-ইন-এর জন্য)
- **Manim** (ঐচ্ছিক, গাণিতিক অ্যানিমেশন স্লাইডের জন্য)

## বিল্ড ও পরীক্ষা

```bash
./gradlew build                    # সম্পূর্ণ বিল্ড
./gradlew check                    # ইউনিট + ফাংশনাল + Cucumber পরীক্ষা
./gradlew test                     # JUnit5 ইউনিট পরীক্ষা (@integration বাদ দেয়)
./gradlew cucumberTest             # Cucumber BDD পরীক্ষা
./gradlew functionalTest           # Gradle TestKit ফাংশনাল পরীক্ষা
./gradlew publishToMavenLocal      # স্থানীয়ভাবে প্রকাশ করুন
```

## সমস্যা সমাধান

| লক্ষণ | সমাধান |
|---------|-----|
| Playwright Chromium পাওয়া যায়নি     | অটো-ডাউনলোডের জন্য একবার চালান, বা `chromiumExecutablePath` সেট করুন |
| Piper TTS কোনো অডিও তৈরি করে না       | `piperExecutablePath` যাচাই করুন, ভয়েস মডেল ইনস্টল আছে কিনা নিশ্চিত করুন |
| `deployCapsule` ব্যর্থ              | যাচাই করুন FFmpeg PATH-এ আছে কিনা (`ffmpeg -version`) |
| Manim স্লাইড রেন্ডার হয়নি          | Manim ছাড়া পরীক্ষার জন্য `manimExecutablePath` কে `noop` সেট করুন |
| ভিডিও ক্যাপচার টাইমআউট              | `playwrightTimeout` বাড়ান (ডিফল্ট 120000ms) |
| মেমোরি শেষ                      | `export GRADLE_OPTS="-Xmx2g"` |

পাইপলাইন বিবরণের জন্য [CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) দেখুন।

## লাইসেন্স

Apache License 2.0 — [LICENSE](../LICENSE) দেখুন।

---

_CCCP Education ইকোসিস্টেমের অংশ — `groupId: education.cccp`।_