<!-- translated from README.md rev 0.0.1-SNAPSHOT -->
# capsule-gradle — उपभोक्ता गाइड

> Gradle प्लगइन जो reveal.js डेक से स्वचालित शैक्षणिक वीडियो कैप्सूल बनाता है।

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.1-SNAPSHOT&color=orange)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=License)](../LICENSE)

- **संस्करण**: `0.0.1-SNAPSHOT` (प्रकाशित नहीं) · **समूह**: `education.cccp` · **प्लगइन आईडी**: `education.cccp.capsule`
- **बिल्ड**: `./gradlew build` · **परीक्षण**: `./gradlew check` (195 यूनिट + 22 Cucumber पास)
- **कवरेज**: Kover XML + HTML रिपोर्ट `check` में एकीकृत

🌐 Languages: [English](README.md) | [中文](README.zh.md) | **हिन्दी** | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## यह क्या करता है

`capsule-gradle` एक reveal.js डेक (`slider-gradle` द्वारा निर्मित) को सिंक्रनाइज़्ड TTS नैरेशन के साथ एक शैक्षणिक वीडियो कैप्सूल में बदलता है — किसी पोस्ट-प्रोडक्शन की आवश्यकता नहीं।

यह पाइपलाइन चार बाहरी टूल को ऑर्केस्ट्रेट करती है:

```
AsciiDoc source → slider-gradle → reveal.js deck + capsule script
                                      ↓
              capsule-gradle → TTS (Piper/espeak) → audio injection → Playwright capture → WebM video
                                      ↓                              ↓
                              Manim animation (optional)      FFmpeg distrib (9:16)
```

CCCP Education मल्टी-प्लगइन इकोसिस्टम का हिस्सा (`groupId: education.cccp`)।

## त्वरित प्रारंभ

### 1. प्लगइन लागू करें

```gradle
plugins {
    id("education.cccp.capsule") version "0.0.1-SNAPSHOT"
}
```

### 2. कैप्सूल कॉन्फ़िगर करें

```gradle
capsule {
    outputDir.set("capsules")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    ttsEngine.set("espeak")
}
```

### 3. वीडियो जनरेट करें

```bash
./gradlew generateCapsuleVideo
```

यह पूर्ण पाइपलाइन चलाता है: स्क्रिप्ट सत्यापन → TTS संश्लेषण → डेक HTML में ऑडियो इंजेक्शन → Playwright हेडलेस कैप्चर → WebM आउटपुट।

### slider के साथ पूर्ण पाइपलाइन

रूट `build.gradle.kts` slider और capsule को एक साथ जोड़ता है:

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
./gradlew capsulevideo    # slider डेक + स्क्रिप्ट → कैप्सूल वीडियो
```

## उपलब्ध कार्य

| कार्य | समूह | विवरण |
|------|-------|-------------|
| `generateCapsuleScript`   | generate  | slider से `*-script.txt` पढ़ता है और कैप्सूल स्क्रिप्ट सत्यापित करता है |
| `generateCapsule`          | generate  | कैप्सूल स्क्रिप्ट से TTS ऑडियो फ़ाइलें बनाता है (Piper/espeak) |
| `generateCapsuleVideo`     | generate  | डेक HTML में TTS ऑडियो इंजेक्ट करता है फिर Playwright द्वारा वीडियो कैप्चर करता है |
| `deployCapsule`            | deploy    | FFmpeg द्वारा कैप्सूल को वर्टिकल 9:16 (TikTok/Shorts) में रीक्रॉप करता है |
| `collectCapsuleContext`    | collect   | कैप्सूल संदर्भ (वीडियो पथ + मेटाडेटा) को JSON में निर्यात करता है, N3 इंजन के लिए |
| `transformCapsuleContext`  | transform | `capsule-context.json` पार्स करता है और डेक की सूची लौटाता है |
| `collectCapsuleRetrieve`   | collect   | संदर्भ JSON से कैप्सूल डेक पुनर्प्राप्त करता है (N3 इंजन अनुबंध) |
| `scaffoldCapsuleContext`   | generate  | टिप्पणियों के साथ डिफ़ॉल्ट `capsule-context.yml` कॉन्फ़िग फ़ाइल तैयार करता है |

## एक्सटेंशन DSL

```gradle
capsule {
    // इनपुट
    outputDir.set("capsule")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    chromiumExecutablePath.set("")

    // TTS
    ttsEngine.set("piper")                    // "piper" या "espeak"
    ttsVoice.set("fr_FR-siwis-medium")
    piperExecutablePath.set("piper")
    ttsFallbackEnabled.set(true)
    espeakVoice.set("fr")
    espeakSpeed.set(150)
    ttsLanguage.set("fr")                     // fr, en, es, de

    // कैप्चर
    viewportWidth.set(1408)
    viewportHeight.set(792)
    playwrightTimeout.set(120_000.0)
    slideDurationSeconds.set(5.0)
    parallelCaptureEnabled.set(false)
    parallelCaptureThreads.set(4)

    // सबटाइटल
    subtitleEnabled.set(false)
    subtitleFormat.set("srt")                 // "srt" या "vtt"
    subtitleBurnIn.set(false)
    subtitleBurnInFontSize.set(24)
    subtitleBurnInFontColor.set("&H00FFFFFF")
    subtitleBurnInOutlineColor.set("&H00000000")
    subtitleBurnInPosition.set("bottom")      // "bottom" या "top"

    // वितरण
    ffmpegExecutablePath.set("ffmpeg")
    distribOutputWidth.set(1080)
    distribOutputHeight.set(1920)

    // Manim एनिमेशन
    manimExecutablePath.set("manim")
    manimQuality.set("l")                     // l, m, h, p, k
    manimScriptsDir.set("src/manim")
    manimOutputDir.set("build/capsule/manim")
    manimParallelRender.set(false)
    manimParallelRenderThreads.set(4)
}
```

### मल्टी-सोर्स कॉन्फ़िगरेशन

कॉन्फ़िगरेशन 4 स्रोतों से हल किया जाता है (प्राथमिकता: CLI > YAML > gradle.properties > ENV):

```bash
./gradlew scaffoldCapsuleContext          # capsule-context.yml जनरेट करें
./gradlew generateCapsuleVideo -Pcapsule.tts.engine=espeak -Pcapsule.manim.quality=h
```

## पूर्वापेक्षित शर्तें

- **Java** 24+ (Kotlin 2.3.20 टूलचेन)
- **Gradle** 9.5+
- **Piper** (TTS, स्थानीय ऑफ़लाइन) — डिफ़ॉल्ट इंजन
- **espeak** (TTS फ़ॉलबैक)
- **Chromium** (पहले रन पर Playwright द्वारा स्वतः डाउनलोड)
- **FFmpeg** (`deployCapsule` 9:16 रीक्रॉप और सबटाइटल बर्न-इन के लिए)
- **Manim** (वैकल्पिक, गणितीय एनिमेशन स्लाइड्स के लिए)

## बिल्ड और परीक्षण

```bash
./gradlew build                    # पूर्ण बिल्ड
./gradlew check                    # यूनिट + फंक्शनल + Cucumber परीक्षण
./gradlew test                     # JUnit5 यूनिट परीक्षण (@integration को बाहर रखता है)
./gradlew cucumberTest             # Cucumber BDD परीक्षण
./gradlew functionalTest           # Gradle TestKit फंक्शनल परीक्षण
./gradlew publishToMavenLocal      # स्थानीय रूप से प्रकाशित करें
```

## समस्या निवारण

| लक्षण | समाधान |
|---------|-----|
| Playwright Chromium नहीं मिला     | ऑटो-डाउनलोड के लिए एक बार चलाएँ, या `chromiumExecutablePath` सेट करें |
| Piper TTS कोई ऑडियो नहीं बनाता       | `piperExecutablePath` जाँचें, सुनिश्चित करें कि वॉइस मॉडल स्थापित है |
| `deployCapsule` विफल              | सत्यापित करें कि FFmpeg PATH पर है (`ffmpeg -version`) |
| Manim स्लाइड्स रेंडर नहीं हुए          | Manim के बिना परीक्षण के लिए `manimExecutablePath` को `noop` सेट करें |
| वीडियो कैप्चर टाइमआउट              | `playwrightTimeout` बढ़ाएँ (डिफ़ॉल्ट 120000ms) |
| मेमोरी समाप्त                      | `export GRADLE_OPTS="-Xmx2g"` |

पाइपलाइन विवरण के लिए [CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) देखें।

## लाइसेंस

Apache License 2.0 — [LICENSE](../LICENSE) देखें।

---

_CCCP Education इकोसिस्टम का हिस्सा — `groupId: education.cccp`।_