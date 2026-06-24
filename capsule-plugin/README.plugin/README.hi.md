<!-- translated from README.md rev 0.0.1-SNAPSHOT -->
# capsule-gradle — प्लगइन आंतरिक

> `capsule-plugin` Gradle प्लगइन के लिए डेवलपर और योगदानकर्ता गाइड।

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.1-SNAPSHOT&color=orange)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=coverage&message=Kover&color=blue)]()
[![License](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=License)](../LICENSE)

- **संस्करण**: `0.0.1-SNAPSHOT` (प्रकाशित नहीं) · **समूह**: `education.cccp` · **प्लगइन आईडी**: `education.cccp.capsule`
- **टूलचेन**: Java 24 · Kotlin 2.3.20 · Gradle 9.5
- **बिल्ड**: `./gradlew build -x test` · **परीक्षण**: `./gradlew check` (195 यूनिट + 22 Cucumber पास) · **कवरेज**: `./gradlew koverReport`

🌐 Languages: [English](README.md) | [中文](README.zh.md) | **हिन्दी** | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## मॉड्यूल लेआउट

```
capsule-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml          # संस्करण सूची
├── doc/
│   └── CAPSULE_ARCHITECTURE.adoc      # पाइपलाइन आर्किटेक्चर (PlantUML)
└── src/
    ├── main/kotlin/capsule/
    │   ├── CapsulePlugin.kt           # प्रवेश बिंदु — slider लागू करता है, कार्य पंजीकृत करता है, विलय कॉन्फ़िग
    │   ├── CapsuleManager.kt          # कार्य पंजीकरण + स्क्रिप्ट पार्सिंग + फ़ैक्ट्री विधियाँ
    │   ├── CapsuleModels.kt           # CapsuleExtension DSL + CapsuleConventions + डेटा मॉडल
    │   ├── CapsuleConfig.kt           # अपरिवर्तनीय कॉन्फ़िग (5 अनुभाग: input, tts, capture, distrib, manim)
    │   ├── CapsuleConfigLoader.kt     # ${VAR} env हल के साथ YAML लोडर
    │   ├── CapsuleConfigMerger.kt     # 4-स्रोत विलय (CLI > YAML > props > ENV)
    │   ├── CapsuleScriptTask.kt       # generateCapsuleScript
    │   ├── CapsuleBuildTask.kt        # generateCapsule (TTS संश्लेषण)
    │   ├── CapsuleVideoTask.kt        # generateCapsuleVideo (ऑडियो इंजेक्शन + Playwright कैप्चर)
    │   ├── CapsuleDistribTask.kt     # deployCapsule (9:16 FFmpeg रीक्रॉप)
    │   ├── CapsuleCompositeContextTask.kt  # collectCapsuleContext (N3 JSON निर्यात)
    │   ├── CapsuleParseContextTask.kt # transformCapsuleContext + collectCapsuleRetrieve
    │   ├── CapsuleScaffoldTask.kt    # scaffoldCapsuleContext
    │   ├── TtsManager.kt             # Piper + espeak TTS एकीकरण
    │   ├── PlaywrightManager.kt      # हेडलेस Chromium कैप्चर
    │   ├── ManimEngine.kt            # Manim रेंडरिंग (NoOp/Impl)
    │   ├── ManimSlideReplacer.kt     # HTML→वीडियो एम्बेड प्रतिस्थापन
    │   ├── ManimVideoMixer.kt        # मक्स Manim MP4 + TTS ऑडियो
    │   ├── ManimParallelRenderer.kt  # समानांतर Manim रेंडरिंग
    │   ├── SubtitleModels.kt         # सबटाइटल डेटा मॉडल
    │   ├── SubtitleTimingCalculator.kt
    │   ├── SrtGenerator.kt           # SRT सबटाइटल निर्माण
    │   ├── VttGenerator.kt           # VTT सबटाइटल निर्माण
    │   ├── SubtitleBurnInService.kt  # FFmpeg सबटाइटल बर्न-इन
    │   ├── HtmlSectionParser.kt       # reveal.js डेक HTML पार्सिंग
    │   ├── AudioConversionUtil.kt    # WAV→MP3 रूपांतरण
    │   ├── MediaProbeUtil.kt         # ffprobe अवधि प्रोबिंग
    │   └── LanguageVoiceMapping.kt    # FR/EN/ES/DE वॉइस मैपिंग
    ├── test/kotlin/capsule/
    │   ├── CapsulePluginTest.kt
    │   ├── scenarios/                 # Cucumber BDD स्टेप परिभाषाएँ + रनर
    │   │   ├── CapsuleStepDefinitions.kt
    │   │   ├── CucumberTestRunner.kt
    │   │   └── CucumberIntegrationTestRunner.kt
    │   └── *Test.kt                   # 17 यूनिट परीक्षण कक्षाएँ
    ├── test/features/
    │   └── capsule.feature            # 22 Cucumber परिदृश्य
    └── functionalTest/kotlin/capsule/
        └── CapsulePluginFunctionalTest.kt
```

## N2 निर्भरता

`capsule-gradle` `slider-gradle` (`education.cccp:slider:0.0.6`) को `compileOnly` द्वारा उपभोग करता है।
अनुबंध फ़ाइल-आधारित है — capsule slider द्वारा निर्मित डेक HTML और स्क्रिप्ट टेक्स्ट फ़ाइलें पढ़ता है,
उन्हें कभी संशोधित नहीं करता। CapsulePlugin रनटाइम पर `education.cccp.slider` को स्वतः लागू करता है यदि उपलब्ध हो।

## प्रमुख लाइब्रेरीज़

| लाइब्रेरी | संस्करण | भूमिका |
|---------|---------|------|
| **Playwright** | 1.52.0 | हेडलेस Chromium — reveal.js नियंत्रण + वीडियो कैप्चर (JVM नेटिव, शून्य npm) |
| **koog-agents** | 0.8.0 | एजेंट ऑर्केस्ट्रेशन के लिए Kotlin DSL (StateGraph / ConditionalEdges) |
| **Jackson** | 2.18.3 | YAML कॉन्फ़िग + JSON सीरियलाइज़ेशन (databind, kotlin, dataformat-yaml) |
| **Kover** | 0.9.8 | कवरेज रिपोर्ट (XML + HTML, `check` में एकीकृत) |
| **Cucumber** | 7.34.3 | BDD परीक्षण (cucumber-java, junit-platform-engine, picocontainer) |
| **Kotlin** | 2.3.20 | प्लगइन DSL |
| **slider** | 0.0.6 | compileOnly — reveal.js डेक + कैप्सूल स्क्रिप्ट स्रोत |

बाहरी टूल (Maven निर्भरताएँ नहीं):
- **Piper** — स्थानीय ऑफ़लाइन TTS इंजन (डिफ़ॉल्ट)
- **espeak** — TTS फ़ॉलबैक इंजन
- **FFmpeg** — 9:16 रीक्रॉप (`deployCapsule`) और सबटाइटल बर्न-इन
- **Manim** — गणितीय एनिमेशन रेंडरिंग (वैकल्पिक, स्क्रिप्ट में `[manim:SceneName]`)
- **Chromium** — पहले रन पर Playwright द्वारा स्वतः डाउनलोड

## परीक्षण मैट्रिक्स

| कार्य | दायरा | विवरण |
|------|-------|---------|
| `test` | JUnit5 यूनिट परीक्षण | `@integration` टैग और `capsule.scenarios.**` को बाहर रखता है |
| `cucumberTest` | Cucumber BDD | 22 परिदृश्य, `forkEvery = 1`, `maxParallelForks = 1`, heap 1g |
| `functionalTest` | Gradle TestKit | प्लगइन अनुप्रयोग कार्यात्मक परीक्षण |
| `check` | सभी | `test` + `cucumberTest` + `functionalTest` पर निर्भर |

Cucumber टैग: `@integration`, `@manim`, `@config`, `@tts`, `@subtitles`, `@burnin`, `@style`, `@parallel`।

कुल परीक्षण: **195 यूनिट + 22 Cucumber = 217 पास** (बेसलाइन सत्र 029)।

## JVM ट्यूनिंग

Cucumber परीक्षण `maxHeapSize = "1g"` का उपयोग करते हैं, `forkEvery = 1` (प्रति परिदृश्य एक ताज़ JVM)
और `maxParallelForks = 1` (स्थिरता के लिए अनुक्रमिक निष्पादन) के साथ।

स्थानीय बिल्ड के लिए:
```bash
export GRADLE_OPTS="-Xmx2g"
```

## बिल्ड कमांड

```bash
./gradlew build                       # पूर्ण बिल्ड (कंपाइल + परीक्षण)
./gradlew build -x test               # केवल कंपाइल
./gradlew check                       # सभी परीक्षण (यूनिट + Cucumber + कार्यात्मक)
./gradlew test                        # केवल JUnit5 यूनिट
./gradlew cucumberTest                # Cucumber BDD
./gradlew functionalTest              # Gradle TestKit
./gradlew koverReport                 # कवरेज रिपोर्ट (XML + HTML)
./gradlew publishToMavenLocal         # स्थानीय प्रकाशन
```

## CI पाइपलाइन

`.github/workflows/test.yml` एक जॉब परिभाषित करता है:
1. **Build & Test** — हर push/PR पर main/master पर `./gradlew check` (≤ 15 मिनट, JDK 24 Temurin, Gradle setup action)

```yaml
jobs:
  test:
    runs-on: ubuntu-latest
    timeout-minutes: 15
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '24', distribution: 'temurin' }
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew check
        working-directory: capsule-plugin
```

## प्रकाशन (NMCP)

**प्रकाशित नहीं** — संस्करण `0.0.1-SNAPSHOT`। POM `maven-publish` और `signing` प्लगइन द्वारा Maven Central के लिए कॉन्फ़िगर किया गया है:

- POM: नाम "Capsule Gradle Plugin", विवरण "Generation automatisee de capsules video
  pedagogiques depuis des decks reveal.js", URL `github.com/cheroliv/capsule-gradle/`
- साइनिंग: `useGpgCmd()` (कॉन्फ़िगर किया गया लेकिन अभी तक निष्पादित नहीं)
- प्रकाशन लक्ष्य: `publishToMavenLocal` उपलब्ध; NMCP `publishAggregationToCentralPortal`
  के लिए `~/.gradle/gradle.properties` में क्रेडेंशियल आवश्यक

तैयार होने पर प्रकाशित करें:
```bash
./gradlew publishAggregationToCentralPortal --no-daemon
```

## आर्किटेक्चर दस्तावेज़

- [CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) — पाइपलाइन
  आर्किटेक्चर PlantUML आरेखों के साथ (वैश्विक पाइपलाइन, घटक, अनुक्रम, तैनाती)
- [.agents/INDEX.adoc](../capsule-plugin/.agents/INDEX.adoc) — EPICs, रोडमैप और शासन
- [AGENT.adoc](../capsule-plugin/AGENT.adoc) — निरपेक्ष नियम (7 नियम, इंक अर्थव्यवस्था नियम)
- [CODE_REVIEW.adoc](../capsule-plugin/CODE_REVIEW.adoc) — कोड समीक्षा (10 क्रियाशील EPICs CR-1→CR-10)

## EPIC स्थिति

CAP-0 से CAP-25 समाप्त। सक्रिय EPICs (देखें `.agents/INDEX.adoc`):

| EPIC | विवरण | स्थिति |
|------|-------------|--------|
| CR-1 | `CapsuleVideoTask.execute()` रिफ़ैक्टर (218→76 लाइनें) | ✅ समाप्त (सत्र 040) |
| CR-2 | थ्रेड सुरक्षा + टाइमआउट `captureSlideParallel` | ⬜ TODO |
| CR-3 | TTS त्रुटि प्रबंधन — क्षरण संकेतित | ⬜ TODO |
| CR-4 | HTML escape सुरक्षा — `injectSubtitleTrack` | ⬜ TODO |
| CR-5 | DRY `CapsuleConfigMerger` (~76 लाइनें डुप्लिकेट) | ⬜ TODO |
| CAP-27 | VTT बर्न-इन (वर्तमान में केवल SRT) | ⬜ TODO |
| CAP-28 | कवरेज अंतराल — PiperTtsEngine 31%, ManimEngineImpl 25% | ⬜ TODO |

## योगदान

1. बिल्ड कंपाइल: `./gradlew build -x test`
2. सभी परीक्षण हरे: `./gradlew check`
3. 7 निरपेक्ष नियमों का पालन करें (देखें [AGENT.adoc](../capsule-plugin/AGENT.adoc)):
   - अनुमति बिना कोई commit/push नहीं
   - DAG N2 — N3 द्वारा आयात योग्य, कभी N3 आयात नहीं
   - Capsule slider का उपभोग (केवल पठन, डेक स्रोत कभी संशोधित नहीं)
   - कोड में शून्य रहस्य/टोकन
4. इंक अर्थव्यवस्था नियम का सम्मान — जब एक ही इनपुट के लिए एक वैध कलाकृति पहले से मौजूद हो तो महंगे ऑपरेशन (TTS, Playwright कैप्चर) कभी न दोहराएँ

## लाइसेंस

Apache License 2.0 — [LICENSE](../LICENSE) देखें।

---

_CCCP Education इकोसिस्टम का हिस्सा — `groupId: education.cccp`।_