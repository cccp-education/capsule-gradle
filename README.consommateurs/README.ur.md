<!-- translated from README.md rev 0.0.1-SNAPSHOT -->
# capsule-gradle — صارفین کی رہنمائی

> Gradle پلگ ان برائے خودکار تیار تعلیمی ویڈیو کیپسولز از reveal.js ڈیکس۔

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.1-SNAPSHOT&color=orange)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=License)](../LICENSE)

- **ورژن**: `0.0.1-SNAPSHOT` (غیر منشور) · **گروہ**: `education.cccp` · **پلگ ان آئی ڈی**: `education.cccp.capsule`
- **بلڈ**: `./gradlew build` · **ٹیسٹ**: `./gradlew check` (195 یونٹ + 22 Cucumber پاس)
- **کوریج**: Kover XML + HTML رپورٹس `check` میں مربوط

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | **اردو**

---

## یہ کیا کرتا ہے

`capsule-gradle` ایک reveal.js ڈیک (`slider-gradle` سے تیار شدہ) کو ہم وقتی TTS بیانی کے ساتھ
ایک تعلیمی ویڈیو کیپسول میں تبدیل کرتا ہے — پوسٹ پروڈکشن کی ضرورت نہیں۔

یہ پائپ لائن چار خارجی ٹولز کو منظم کرتا ہے:

```
AsciiDoc source → slider-gradle → reveal.js deck + capsule script
                                      ↓
              capsule-gradle → TTS (Piper/espeak) → audio injection → Playwright capture → WebM video
                                      ↓                              ↓
                              Manim animation (optional)      FFmpeg distrib (9:16)
```

CCCP Education ملٹی پلگ ان ماحولیاتی نظام کا حصہ (`groupId: education.cccp`)۔

## فوری آغاز

### 1. پلگ ان لگائیں

```gradle
plugins {
    id("education.cccp.capsule") version "0.0.1-SNAPSHOT"
}
```

### 2. کیپسول تشکیل دیں

```gradle
capsule {
    outputDir.set("capsules")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    ttsEngine.set("espeak")
}
```

### 3. ویڈیو تیار کریں

```bash
./gradlew generateCapsuleVideo
```

یہ مکمل پائپ لائن چلاتا ہے: اسکرپٹ توثیق → TTS ترکیب → ڈیک HTML میں آڈیو انجیکشن → Playwright ہیڈ لیس کیپچر → WebM آؤٹ پٹ۔

### slider کے ساتھ مکمل پائپ لائن

روٹ `build.gradle.kts` slider اور capsule کو ایک ساتھ جوڑتا ہے:

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
./gradlew capsulevideo    # slider ڈیک + اسکرپٹ → کیپسول ویڈیو
```

## دستیاب کام

| کام | گروہ | تفصیل |
|------|-------|-------------|
| `generateCapsuleScript`   | generate  | slider سے `*-script.txt` پڑھتا ہے اور کیپسول اسکرپٹ توثیق کرتا ہے |
| `generateCapsule`          | generate  | کیپسول اسکرپٹس سے TTS آڈیو فائلیں تیار کرتا ہے (Piper/espeak) |
| `generateCapsuleVideo`     | generate  | ڈیک HTML میں TTS آڈیو انجیکٹ کرتا ہے پھر Playwright سے ویڈیو کیپچر کرتا ہے |
| `deployCapsule`            | deploy    | FFmpeg سے کیپسولز کو عمودی 9:16 (TikTok/Shorts) میں دوبارہ تراشتا ہے |
| `collectCapsuleContext`    | collect   | کیپسول سیاق و سباق (ویڈیو راستے + میٹا ڈیٹا) JSON میں برآمد کرتا ہے، N3 انجن کے لیے |
| `transformCapsuleContext`  | transform | `capsule-context.json` پارس کرتا ہے اور ڈیکس کی فہرست لوٹاتا ہے |
| `collectCapsuleRetrieve`   | collect   | سیاق JSON سے کیپسول ڈیکس بازیافت کرتا ہے (N3 انجن معاہدہ) |
| `scaffoldCapsuleContext`   | generate  | تبصرے کے ساتھ ایک طے شدہ `capsule-context.yml` کنفگ فائل تیار کرتا ہے |

## ایکسٹینشن DSL

```gradle
capsule {
    // ان پٹ
    outputDir.set("capsule")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    chromiumExecutablePath.set("")

    // TTS
    ttsEngine.set("piper")                    // "piper" یا "espeak"
    ttsVoice.set("fr_FR-siwis-medium")
    piperExecutablePath.set("piper")
    ttsFallbackEnabled.set(true)
    espeakVoice.set("fr")
    espeakSpeed.set(150)
    ttsLanguage.set("fr")                     // fr, en, es, de

    // کیپچر
    viewportWidth.set(1408)
    viewportHeight.set(792)
    playwrightTimeout.set(120_000.0)
    slideDurationSeconds.set(5.0)
    parallelCaptureEnabled.set(false)
    parallelCaptureThreads.set(4)

    // ذیلی عنوانات
    subtitleEnabled.set(false)
    subtitleFormat.set("srt")                 // "srt" یا "vtt"
    subtitleBurnIn.set(false)
    subtitleBurnInFontSize.set(24)
    subtitleBurnInFontColor.set("&H00FFFFFF")
    subtitleBurnInOutlineColor.set("&H00000000")
    subtitleBurnInPosition.set("bottom")      // "bottom" یا "top"

    // تقسیم
    ffmpegExecutablePath.set("ffmpeg")
    distribOutputWidth.set(1080)
    distribOutputHeight.set(1920)

    // Manim اینیمیشن
    manimExecutablePath.set("manim")
    manimQuality.set("l")                     // l, m, h, p, k
    manimScriptsDir.set("src/manim")
    manimOutputDir.set("build/capsule/manim")
    manimParallelRender.set(false)
    manimParallelRenderThreads.set(4)
}
```

### ملٹی سورس کنفگریشن

کنفگریشن 4 ذرائع سے حل کی جاتی ہے (ترجیح: CLI > YAML > gradle.properties > ENV):

```bash
./gradlew scaffoldCapsuleContext          # capsule-context.yml تیار کریں
./gradlew generateCapsuleVideo -Pcapsule.tts.engine=espeak -Pcapsule.manim.quality=h
```

## ضروریات

- **Java** 24+ (Kotlin 2.3.20 ٹول چین)
- **Gradle** 9.5+
- **Piper** (TTS، مقامی آف لائن) — طے شدہ انجن
- **espeak** (TTS فال بیک)
- **Chromium** (پہلی رن پر Playwright سے خود بخود ڈاؤن لوڈ)
- **FFmpeg** (`deployCapsule` 9:16 دوبارہ تراش اور ذیلی عنوان برن اِن کے لیے)
- **Manim** (اختیاری، ریاضیاتی اینیمیشن سلائڈس کے لیے)

## بلڈ اور ٹیسٹ

```bash
./gradlew build                    # مکمل بلڈ
./gradlew check                    # یونٹ + فنکشنل + Cucumber ٹیسٹ
./gradlew test                     # JUnit5 یونٹ ٹیسٹ (@integration خارج)
./gradlew cucumberTest             # Cucumber BDD ٹیسٹ
./gradlew functionalTest           # Gradle TestKit فنکشنل ٹیسٹ
./gradlew publishToMavenLocal      # مقامی شائع کریں
```

## مسائل کا حل

| علامت | حل |
|---------|-----|
| Playwright Chromium نہیں ملا     | آٹو ڈاؤن لوڈ کے لیے ایک بار چلائیں، یا `chromiumExecutablePath` سیٹ کریں |
| Piper TTS کوئی آڈیو نہیں بناتا       | `piperExecutablePath` جانچیں، یقینی کریں کہ آواز ماڈل انسٹال ہے |
| `deployCapsule` ناکام              | تصدیق کریں کہ FFmpeg PATH پر ہے (`ffmpeg -version`) |
| Manim سلائڈز رینڈر نہیں ہوئے          | Manim کے بغیر جانچ کے لیے `manimExecutablePath` کو `noop` سیٹ کریں |
| ویڈیو کیپچر ٹائم آؤٹ              | `playwrightTimeout` بڑھائیں (طے شدہ 120000ms) |
| میموری ختم                      | `export GRADLE_OPTS="-Xmx2g"` |

پائپ لائن کی تفصیلات کے لیے [CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) دیکھیں۔

## لائسنس

Apache License 2.0 — [LICENSE](../LICENSE) دیکھیں۔

---

_CCCP Education ماحولیاتی نظام کا حصہ — `groupId: education.cccp`۔_