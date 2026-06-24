<!-- translated from README.md rev 0.0.1-SNAPSHOT -->
# capsule-gradle — প্লাগইন অভ্যন্তরীণ

> `capsule-plugin` Gradle প্লাগইনের জন্য ডেভেলপার ও অবদানকারী গাইড।

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.1-SNAPSHOT&color=orange)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=coverage&message=Kover&color=blue)]()
[![License](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=License)](../LICENSE)

- **সংস্করণ**: `0.0.1-SNAPSHOT` (প্রকাশিত নয়) · **গ্রুপ**: `education.cccp` · **প্লাগইন আইডি**: `education.cccp.capsule`
- **টুলচেইন**: Java 24 · Kotlin 2.3.20 · Gradle 9.5
- **বিল্ড**: `./gradlew build -x test` · **পরীক্ষা**: `./gradlew check` (195 ইউনিট + 22 Cucumber পাস) · **কভারেজ**: `./gradlew koverReport`

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | **বাংলা** | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## মডিউল বিন্যাস

```
capsule-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml          # সংস্করণ তালিকা
├── doc/
│   └── CAPSULE_ARCHITECTURE.adoc      # পাইপলাইন আর্কিটেকচার (PlantUML)
└── src/
    ├── main/kotlin/capsule/
    │   ├── CapsulePlugin.kt           # এন্ট্রি পয়েন্ট — slider প্রয়োগ করে, টাস্ক নিবন্ধন, কনফিগ একীভূতকরণ
    │   ├── CapsuleManager.kt          # টাস্ক নিবন্ধন + স্ক্রিপ্ট পার্সিং + ফ্যাক্টরি পদ্ধতি
    │   ├── CapsuleModels.kt           # CapsuleExtension DSL + CapsuleConventions + ডেটা মডেল
    │   ├── CapsuleConfig.kt           # অপরিবর্তনীয় কনফিগ (5 বিভাগ: input, tts, capture, distrib, manim)
    │   ├── CapsuleConfigLoader.kt     # ${VAR} env সমাধান সহ YAML লোডার
    │   ├── CapsuleConfigMerger.kt     # 4-সোর্স একীভূতকরণ (CLI > YAML > props > ENV)
    │   ├── CapsuleScriptTask.kt       # generateCapsuleScript
    │   ├── CapsuleBuildTask.kt        # generateCapsule (TTS সংশ্লেষণ)
    │   ├── CapsuleVideoTask.kt        # generateCapsuleVideo (অডিও ইনজেকশন + Playwright ক্যাপচার)
    │   ├── CapsuleDistribTask.kt     # deployCapsule (9:16 FFmpeg রিক্রপ)
    │   ├── CapsuleCompositeContextTask.kt  # collectCapsuleContext (N3 JSON রপ্তানি)
    │   ├── CapsuleParseContextTask.kt # transformCapsuleContext + collectCapsuleRetrieve
    │   ├── CapsuleScaffoldTask.kt    # scaffoldCapsuleContext
    │   ├── TtsManager.kt             # Piper + espeak TTS একীকরণ
    │   ├── PlaywrightManager.kt      # হেডলেস Chromium ক্যাপচার
    │   ├── ManimEngine.kt            # Manim রেন্ডারিং (NoOp/Impl)
    │   ├── ManimSlideReplacer.kt     # HTML→ভিডিও এম্বেড প্রতিস্থাপন
    │   ├── ManimVideoMixer.kt        # মাক্স Manim MP4 + TTS অডিও
    │   ├── ManimParallelRenderer.kt  # সমান্তরাল Manim রেন্ডারিং
    │   ├── SubtitleModels.kt         # সাবটাইটেল ডেটা মডেল
    │   ├── SubtitleTimingCalculator.kt
    │   ├── SrtGenerator.kt           # SRT সাবটাইটেল জেনারেশন
    │   ├── VttGenerator.kt           # VTT সাবটাইটেল জেনারেশন
    │   ├── SubtitleBurnInService.kt  # FFmpeg সাবটাইটেল বার্ন-ইন
    │   ├── HtmlSectionParser.kt       # reveal.js ডেক HTML পার্সিং
    │   ├── AudioConversionUtil.kt    # WAV→MP3 রূপান্তর
    │   ├── MediaProbeUtil.kt         # ffprobe সময়কাল প্রোবিং
    │   └── LanguageVoiceMapping.kt    # FR/EN/ES/DE ভয়েস ম্যাপিং
    ├── test/kotlin/capsule/
    │   ├── CapsulePluginTest.kt
    │   ├── scenarios/                 # Cucumber BDD স্টেপ ডেফিনিশন + রানার
    │   │   ├── CapsuleStepDefinitions.kt
    │   │   ├── CucumberTestRunner.kt
    │   │   └── CucumberIntegrationTestRunner.kt
    │   └── *Test.kt                   # 17 ইউনিট টেস্ট ক্লাস
    ├── test/features/
    │   └── capsule.feature            # 22 Cucumber পরিস্থিতি
    └── functionalTest/kotlin/capsule/
        └── CapsulePluginFunctionalTest.kt
```

## N2 নির্ভরতা

`capsule-gradle` `slider-gradle` (`education.cccp:slider:0.0.6`) কে `compileOnly` এর মাধ্যমে গ্রহণ করে।
চুক্তিটি ফাইল-ভিত্তিক — capsule slider দ্বারা নির্মিত ডেক HTML এবং স্ক্রিপ্ট টেক্সট ফাইলগুলি পড়ে,
কখনও সেগুলি পরিবর্তন করে না। CapsulePlugin রানটাইমে `education.cccp.slider` স্বয়ংক্রিয়ভাবে প্রয়োগ করে যদি উপলব্ধ থাকে।

## মূল লাইব্রেরি

| লাইব্রেরি | সংস্করণ | ভূমিকা |
|---------|---------|------|
| **Playwright** | 1.52.0 | হেডলেস Chromium — reveal.js নিয়ন্ত্রণ + ভিডিও ক্যাপচার (JVM নেটিভ, শূন্য npm) |
| **koog-agents** | 0.8.0 | এজেন্ট অর্কেস্ট্রেশনের জন্য Kotlin DSL (StateGraph / ConditionalEdges) |
| **Jackson** | 2.18.3 | YAML কনফিগ + JSON সিরিয়ালাইজেশন (databind, kotlin, dataformat-yaml) |
| **Kover** | 0.9.8 | কভারেজ রিপোর্ট (XML + HTML, `check`-এ সংযুক্ত) |
| **Cucumber** | 7.34.3 | BDD পরীক্ষা (cucumber-java, junit-platform-engine, picocontainer) |
| **Kotlin** | 2.3.20 | প্লাগইন DSL |
| **slider** | 0.0.6 | compileOnly — reveal.js ডেক + ক্যাপসুল স্ক্রিপ্ট উৎস |

বাহ্যিক টুল (Maven নির্ভরতা নয়):
- **Piper** — স্থানীয় অফলাইন TTS ইঞ্জিন (ডিফল্ট)
- **espeak** — TTS ফলব্যাক ইঞ্জিন
- **FFmpeg** — 9:16 রিক্রপ (`deployCapsule`) এবং সাবটাইটেল বার্ন-ইন
- **Manim** — গাণিতিক অ্যানিমেশন রেন্ডারিং (ঐচ্ছিক, স্ক্রিপ্টে `[manim:SceneName]`)
- **Chromium** — প্রথম রানে Playwright দ্বারা স্বয়ংক্রিয়ভাবে ডাউনলোড

## পরীক্ষা ম্যাট্রিক্স

| টাস্ক | সুযোগ | বিবরণ |
|------|-------|---------|
| `test` | JUnit5 ইউনিট পরীক্ষা | `@integration` ট্যাগ এবং `capsule.scenarios.**` বাদ দেয় |
| `cucumberTest` | Cucumber BDD | 22 পরিস্থিতি, `forkEvery = 1`, `maxParallelForks = 1`, heap 1g |
| `functionalTest` | Gradle TestKit | প্লাগইন প্রয়োগ কার্যকরী পরীক্ষা |
| `check` | সব | `test` + `cucumberTest` + `functionalTest` এর উপর নির্ভরশীল |

Cucumber ট্যাগ: `@integration`, `@manim`, `@config`, `@tts`, `@subtitles`, `@burnin`, `@style`, `@parallel`।

মোট পরীক্ষা: **195 ইউনিট + 22 Cucumber = 217 পাস** (বেসলাইন সেশন 029)।

## JVM টিউনিং

Cucumber পরীক্ষাগুলি `maxHeapSize = "1g"` ব্যবহার করে, `forkEvery = 1` (প্রতি পরিস্থিতিতে একটি নতুন JVM)
এবং `maxParallelForks = 1` (স্থিরতার জন্য ক্রমিক সম্পাদন) সহ।

স্থানীয় বিল্ডের জন্য:
```bash
export GRADLE_OPTS="-Xmx2g"
```

## বিল্ড কমান্ড

```bash
./gradlew build                       # সম্পূর্ণ বিল্ড (কম্পাইল + পরীক্ষা)
./gradlew build -x test               # শুধু কম্পাইল
./gradlew check                       # সব পরীক্ষা (ইউনিট + Cucumber + কার্যকরী)
./gradlew test                        # শুধু JUnit5 ইউনিট
./gradlew cucumberTest                # Cucumber BDD
./gradlew functionalTest              # Gradle TestKit
./gradlew koverReport                 # কভারেজ রিপোর্ট (XML + HTML)
./gradlew publishToMavenLocal         # স্থানীয় প্রকাশনা
```

## CI পাইপলাইন

`.github/workflows/test.yml` একটি কাজ নির্ধারণ করে:
1. **Build & Test** — প্রতিটি push/PR-এ main/master তে `./gradlew check` (≤ 15 মিনিট, JDK 24 Temurin, Gradle setup action)

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

## প্রকাশনা (NMCP)

**প্রকাশিত নয়** — সংস্করণ `0.0.1-SNAPSHOT`। POM `maven-publish` এবং `signing` প্লাগইনের মাধ্যমে Maven Central-এর জন্য কনফিগার করা:

- POM: নাম "Capsule Gradle Plugin", বিবরণ "Generation automatisee de capsules video
  pedagogiques depuis des decks reveal.js", URL `github.com/cheroliv/capsule-gradle/`
- স্বাক্ষর: `useGpgCmd()` (কনফিগার করা কিন্তু এখনও কার্যকর নয়)
- প্রকাশনা লক্ষ্য: `publishToMavenLocal` উপলব্ধ; NMCP `publishAggregationToCentralPortal`
  এর জন্য `~/.gradle/gradle.properties`-এ শংসাপত্র প্রয়োজন

প্রস্তুত হলে প্রকাশ করুন:
```bash
./gradlew publishAggregationToCentralPortal --no-daemon
```

## আর্কিটেকচার নথি

- [CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) — পাইপলাইন
  আর্কিটেকচার PlantUML রেখাচিত্র সহ (বৈশ্বিক পাইপলাইন, উপাদান, অনুক্রম, স্থাপনা)
- [.agents/INDEX.adoc](../capsule-plugin/.agents/INDEX.adoc) — EPICs, রোডম্যাপ এবং শাসন
- [AGENT.adoc](../capsule-plugin/AGENT.adoc) — অপরিবর্তনীয় নিয়ম (7 নিয়ম, কালি অর্থনীতি সূত্র)
- [CODE_REVIEW.adoc](../capsule-plugin/CODE_REVIEW.adoc) — কোড পর্যালোচনা (10 কার্যকর EPICs CR-1→CR-10)

## EPIC অবস্থা

CAP-0 থেকে CAP-25 সমাপ্ত। সক্রিয় EPICs (`.agents/INDEX.adoc` দেখুন):

| EPIC | বিবরণ | অবস্থা |
|------|-------------|--------|
| CR-1 | রিফ্যাক্টর `CapsuleVideoTask.execute()` (218→76 লাইন) | ✅ সমাপ্ত (সেশন 040) |
| CR-2 | থ্রেড সুরক্ষা + টাইমআউট `captureSlideParallel` | ⬜ TODO |
| CR-3 | TTS ত্রুটি পরিচালনা — অবনয়ন সংকেতকৃত | ⬜ TODO |
| CR-4 | HTML escape নিরাপত্তা — `injectSubtitleTrack` | ⬜ TODO |
| CR-5 | DRY `CapsuleConfigMerger` (~76 লাইন নকল) | ⬜ TODO |
| CAP-27 | VTT বার্ন-ইন (বর্তমানে শুধু SRT) | ⬜ TODO |
| CAP-28 | কভারেজ শূন্যস্থান — PiperTtsEngine 31%, ManimEngineImpl 25% | ⬜ TODO |

## অবদান

1. বিল্ড কম্পাইল হয়: `./gradlew build -x test`
2. সব পরীক্ষা সবুজ: `./gradlew check`
3. 7টি অপরিবর্তনীয় নিয়ম অনুসরণ করুন ([AGENT.adoc](../capsule-plugin/AGENT.adoc) দেখুন):
   - অনুমতি ছাড়া কোনো commit/push নয়
   - DAG N2 — N3 দ্বারা আমদানিযোগ্য, কখনও N3 আমদানি করে না
   - Capsule slider গ্রহণ করে (শুধু পঠন, উৎস ডেক কখনও পরিবর্তন করে না)
   - কোডে শূন্য গোপন/টোকেন
4. কালি অর্থনীতি সূত্র মেনে চলুন — একই ইনপুটের জন্য বৈধ আর্টিফ্যাক্ট বিদ্যমান থাকলে ব্যয়বহুল ক্রিয়াকলাপ (TTS, Playwright ক্যাপচার) পুনরায় করবেন না

## লাইসেন্স

Apache License 2.0 — [LICENSE](../LICENSE) দেখুন।

---

_CCCP Education ইকোসিস্টেমের অংশ — `groupId: education.cccp`।_