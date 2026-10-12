<!-- translated from README.md rev 0.0.10 -->
# capsule-gradle — प्लगइन की आंतरिक जानकारी

> `capsule-plugin` Gradle प्लगइन के लिए डेवलपर एवं योगदानकर्ता मार्गदर्शिका।

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.10&color=green)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![कवरेज](https://img.shields.io/static/v1?label=%E0%A4%95%E0%A4%B5%E0%A4%B0%E0%A5%87%E0%A4%9C&message=Kover&color=blue)]()
[![लाइसेंस](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=%E0%A4%B2%E0%A4%BE%E0%A4%87%E0%A4%B8%E0%A5%87%E0%A4%82%E0%A4%B8)](../../LICENSE)

- **संस्करण**: `0.0.10` (Maven Central) · **समूह**: `education.cccp` · **प्लगइन ID**: `education.cccp.capsule`
- **टूलचेन**: Java 25 · Kotlin 2.4.10 · Gradle 9.8.1
- **बिल्ड**: `./gradlew build -x test` · **टेस्ट**: `./gradlew check` (यूनिट + TestKit द्वारा कार्यात्मक; Cucumber के लिए `-PrunCucumber`) · **कवरेज**: `./gradlew koverReport`

🌐 भाषाएँ: [English](../README.md) | [中文](README.zh.md) | **हिन्दी** | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## यह क्या करता है

`capsule-gradle` एक reveal.js डेक (`slider-gradle` द्वारा निर्मित) को सिंक्रनाइज़्ड
वीडियो कैप्सूल में बदलता है — प्रति स्लाइड TTS वॉइस-ओवर, वीडियो कैप्चर, सबटाइटल,
प्रारूप रूपांतरण, ऑडियो पोस्ट-प्रोडक्शन, गुणवत्ता नियंत्रण और एक वर्टिकल
शॉर्ट-फ़ॉर्म/वायरल पाइपलाइन — बिना किसी मानवीय पोस्ट-प्रोडक्शन के। यह एक N2 स्तर का
Gradle प्लगइन है (N3 रनर द्वारा उपभोग किया जाता है, स्वयं N3 नहीं)।

## मॉड्यूल संरचना

```
capsule-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml            # आंतरिक संस्करण कैटलॉग
├── doc/CAPSULE_ARCHITECTURE.adoc        # पाइपलाइन आर्किटेक्चर (PlantUML)
└── src/
    ├── main/kotlin/capsule/
    │   ├── CapsulePlugin.kt             # प्रवेश बिंदु — slider लागू करता है, टास्क पंजीकृत करता है, config मर्ज करता है
    │   ├── CapsuleManager.kt            # टास्क पंजीकरण + इंजन फ़ैक्टरियाँ (resolve*)
    │   ├── CapsuleModels.kt             # CapsuleExtension DSL + CapsuleConventions
    │   ├── CapsuleConfig.kt             # अपरिवर्तनीय config — 18 अनुभाग
    │   ├── CapsuleConfigLoader.kt       # ${VAR} env रिज़ॉल्यूशन के साथ YAML लोडर
    │   ├── CapsuleConfigMerger.kt       # 4-स्रोत मर्ज (CLI > YAML > gradle.properties > ENV)
    │   ├── CapsuleConfigLogger.kt       # संरचित config लॉगिंग (CR-8)
    │   ├── Capsule{Messages}*.kt        # i18n संसाधन रिज़ॉल्यूशन (10 भाषाएँ)
    │   ├── TtsManager.kt                # Piper + espeak + Fish Audio TTS एकीकरण
    │   ├── PlaywrightManager.kt         # हेडलेस Chromium कैप्चर
    │   ├── CaptureResolver.kt           # PLAYWRIGHT / SCREENSHOT / REMOTION प्रेषण
    │   ├── ScreenshotPlanner.kt         # शुद्ध योजना: प्रति स्लाइड PNG → WebM → concat demuxer
    │   ├── Remotion*.kt                 # Remotion कैप्चर रणनीति (Node/@remotion/renderer)
    │   ├── Manim{Engine,SlideReplacer,VideoMixer,ParallelRenderer}.kt  # Manim संवर्धन
    │   ├── Mp4ConversionCommand.kt      # WebM→MP4 हेतु शुद्ध FFmpeg argv (+ BT.709, opt-in)
    │   ├── VideoFormatConverter.kt      # WebM → MP4/H.264 ट्रांसकोड (CAP-MP4)
    │   ├── FormatConversion.kt          # WEBM / MP4 / BOTH प्रेषण
    │   ├── VideoDestinationResolver.kt  # संस्करणित गंतव्य (TIMESTAMP / INCREMENTAL)
    │   ├── Subtitle{BurnInCommand,BurnInService,Cartoner,TextNormalizer,
    │   │            TimingCalculator,TimingDistributor,Models}.kt + SrtGenerator.kt + VttGenerator.kt
    │   ├── Html{SectionParser,Escape}.kt
    │   ├── AudioConversionUtil.kt       # WAV→MP3 रूपांतरण
    │   ├── MediaProbeUtil.kt            # ffprobe द्वारा अवधि जाँच
    │   ├── RenderSignature.kt           # रेंडर हस्ताक्षर + .sig पुनरुपयोग (PRT-06)
    │   ├── ProcessRunner.kt / FileReplace.kt / StrictModeGuard.kt
    │   ├── ai/            # codebase LlmBuildService तक LLM सेतु (CapsuleLlmService, capsuleAiSmokeTest)
    │   ├── anim/          # Remotion टाइमिंग पोर्ट (RemotionTiming)
    │   ├── audio/         # CAP-AUDIO: AudioPostCommand (शुद्ध argv), AudioPostProcessor (+NoOp), BGM/loudnorm/ducking
    │   ├── chapters/      # अध्याय मार्कर, CardRenderer, YouTubeChaptersFormatter
    │   ├── ci/            # CucumberTestGuard (CR-10), ProcessMigrationGuard, OssVocabularyGuard
    │   ├── context/       # संवर्धित संदर्भ: CapsuleContext(Builder), Doc/Scenario/Glossary लोडर, ProvenanceTracker
    │   ├── feed/          # वक्ता-नोट्स निष्कर्षण अनुबंध (SLD-11) + संयुक्त रजिस्ट्रार
    │   ├── i18n/          # CapsuleMessages (10 भाषा बंडल)
    │   ├── multilang/     # CAP-29: MultiLanguageResolver, VoiceMapping, CapsuleVideoPlan(ner), AllLanguages रनर
    │   ├── pipeline/      # CAP-ARCH-3: koog CapsulePipelineGraph, CapsuleState, CapsuleLlm (+ChatModel), ContentPlanValidator
    │   ├── podcast/       # CAP-PODCAST: PodcastConcatenator (+Impl/NoOp), PodcastConcatCommand
    │   ├── preview/       # dry-run (capture.previewOnly — केवल PNG)
    │   ├── publish/       # VideoDescriptionBuilder (YouTube गार्ड)
    │   ├── qc/            # CAP-QC: ContentPreflight (PRT-05), QcBoard/योजनाकार + RenderQcBoardsTask (PRT-08)
    │   ├── timeline/      # PRT-07: नियतात्मक timeline (पूर्ण फ़्रेम, lead/tail/avance)
    │   ├── transcript/    # CAP-TRANSCRIPT: TranscriptPlan/Builder, ChatModelTranscriptEnhancer
    │   ├── validation/    # VideoDurationValidator + VideoIntegrityChecker (PRT-04)
    │   ├── viral/         # CAP-VIRAL: storyboard, deck builder, campaign bundle/gate, thumbnail, कराओके
    │   └── voice/         # CAP-QC-VOICE: TextNormalizer, WordAligner, FidelityScorer, SnrEstimator,
    │                      #   PronunciationLexicon, NumberSpeller, SpeechRecognizer (पोर्ट), TakeSelector
    ├── main/resources/capsule/
    │   ├── i18n/Messages[_xx].properties   # 10 भाषाएँ + आधार
    │   └── remotion/                        # Remotion संरचना (Capsule.jsx, Root.jsx, render.mjs)
    ├── test/kotlin/capsule/
    │   ├── scenarios/                       # Cucumber स्टेप परिभाषाएँ + प्रति-feature रनर
    │   └── **/*Test.kt                      # यूनिट टेस्ट
    ├── test/features/                       # *.feature — Cucumber परिदृश्य (44 फ़ाइलें)
    └── functionalTest/kotlin/capsule/       # Gradle TestKit कार्यात्मक टेस्ट
```

## Gradle टास्क

| समूह | टास्क |
|------|-------|
| मुख्य पाइपलाइन | `generateCapsuleScript`, `generateCapsule` (TTS), `generateCapsuleVideo` (ऑडियो इंजेक्शन + कैप्चर), `generateCapsuleVideoAllLanguages`, `deployCapsule` (9:16 रीक्रॉप), `distributeCapsuleVideo` (संस्करणित गंतव्य) |
| LLM सामग्री | `generateCapsuleContent`, `capsuleAiSmokeTest`, `collectCapsuleAugmentedContext`, `generateCapsuleContentAndVideos` |
| संदर्भ | `collectCapsuleContext`, `transformCapsuleContext`, `collectCapsuleRetrieve`, `scaffoldCapsuleContext` |
| आउटपुट | `generateCapsuleTranscript`, `generateCapsuleChapters`, `generateCapsulePodcast`, `validateCapsuleVideoDuration` |
| गुणवत्ता (opt-in) | `preflightCapsuleContent`, `renderQcBoards`, `verifyCapsuleVoice`, `selectBestVoiceTake` |
| वायरल (opt-in) | `generateViralCampaign`, `normalizeViralStoryboard`, `renderViralVariants`, `renderViralThumbnails`, `renderViralSubtitles`, `validateViralCampaign` |
| संयुक्त feed | `translateAndExtractSpeakerNotes`, `translateAndGenerateCapsuleVideos` |

## निर्भरताएँ

`capsule-gradle` `slider-gradle` (`education.cccp:slider:0.0.19`) को `compileOnly` के रूप में उपभोग करता है —
यह एक फ़ाइल-आधारित अनुबंध है (capsule डेक HTML और स्क्रिप्ट टेक्स्ट पढ़ता है, उन्हें कभी नहीं बदलता)।
`CapsulePlugin` रनटाइम पर उपलब्ध होने पर `education.cccp.slider` स्वतः लागू करता है।

CAP-ARCH-1 से, capsule codebase LLM सोकल (`education.cccp:codebase-plugin`) को
`implementation` के रूप में भी उपभोग करता है — `LlmBuildService` सेतु `generateCapsuleContent`
द्वारा प्रयुक्त `ChatModel` प्रदान करता है, टेस्ट के लिए `-Pollama.baseUrl` द्वारा mock-LLM
फ़ॉलबैक के साथ (शून्य नेटवर्क, शून्य पूल)।

| लाइब्रेरी | संस्करण | भूमिका |
|-----------|---------|--------|
| **Playwright** | 1.52.0 | हेडलेस Chromium — reveal.js नियंत्रण + वीडियो कैप्चर (नेटिव JVM, शून्य npm) |
| **koog-agents** | 1.3.0 | एजेंट ऑर्केस्ट्रेशन हेतु Kotlin DSL (StateGraph / ConditionalEdges) — `CapsulePipelineGraph` |
| **langchain4j** | 1.22.0 | `ChatModel` अडैप्टर (LLM सेतु, ट्रांसक्रिप्ट संवर्धक) — संस्करण BOM से |
| **Jackson** | 2.21.2 | YAML config + JSON सीरियलाइज़ेशन (databind, kotlin, dataformat-yaml) |
| **Kover** | 0.9.8 | कवरेज रिपोर्ट (XML + HTML, `check` में जुड़ी) |
| **Cucumber** | 7.34.3 | BDD टेस्ट (cucumber-java, junit-platform-engine, picocontainer) |
| **Kotlin** | 2.4.10 | प्लगइन DSL |
| **codebase-plugin** | 0.0.17 | N1 LLM सोकल — `LlmBuildService` सेतु (`capsule.ai`) |
| **codebase-contracts** | 0.0.2 | N0 अनुबंध — CompositeContext / ContextChannel / ChannelBudget (`capsule.context`) |
| **i18n-contracts** | 0.0.4 | N0 अनुबंध — `LanguageCatalog` / `TranslationService` (`capsule.i18n`) |
| **slider** | 0.0.19 | compileOnly — reveal.js डेक + capsule स्क्रिप्ट स्रोत |

बाहरी उपकरण (Maven निर्भरताएँ नहीं): **Piper** (स्थानीय ऑफ़लाइन TTS, डिफ़ॉल्ट),
**espeak-ng** (TTS फ़ॉलबैक इंजन), **FFmpeg** (रीक्रॉप, burn-in, MP4, ऑडियो पोस्ट),
**Manim** (वैकल्पिक एनिमेटेड आरेख, `[manim:SceneName]`), **Chromium** (पहले रन पर
Playwright द्वारा स्वतः डाउनलोड), **Node** (Remotion रणनीति)।

## कॉन्फ़िगरेशन

`CapsuleConfig` में **18 अनुभाग** हैं: `input`, `tts`, `capture`, `distrib`, `manim`,
`output`, `strictMode`, `context`, `validation`, `qc`, `audioPost`, `transcript`,
`remotion`, `podcast`, `preview`, `chapters`, `viral`, `voice`।

हर मान **4 स्रोतों** से हल होता है (प्राथमिकता **CLI > YAML > gradle.properties > ENV**):

```bash
# CLI
./gradlew generateCapsuleVideo -Poutput.format=mp4 -Pcapture.previewOnly=true

# gradle.properties / ENV
capsule.output.format=mp4
CAPSULE_OUTPUT_FORMAT=mp4
```

नई क्षमताएँ पश्च-संगतता हेतु **opt-in (डिफ़ॉल्ट `false`)** हैं।

## टेस्ट मैट्रिक्स

| टास्क | दायरा | विवरण |
|-------|-------|-------|
| `test` | JUnit5 यूनिट टेस्ट | `capsule.scenarios.**` को छोड़ता है। `@integration` टेस्ट *नहीं* फ़िल्टर होते: बाहरी उपकरण (Chromium, espeak, FFmpeg, Manim) अनुपस्थित होने पर वे स्वयं स्किप हो जाते हैं |
| `cucumberTest` | Cucumber BDD | `src/test/features` के अंतर्गत feature (44 `.feature` फ़ाइलें), `forkEvery = 1`, `maxParallelForks = 1`, heap 1g। `-PrunCucumber` या `CI=true` के बिना स्किप (CR-10) |
| `functionalTest` | Gradle TestKit | प्लगइन अनुप्रयोग के कार्यात्मक टेस्ट |
| `check` | सभी | `test` + `cucumberTest` + `functionalTest` पर निर्भर |

Cucumber टैग: `@ai`, `@architecture`, `@audio`, `@boundary`, `@burnin`, `@capsule`, `@capture`, `@ci`, `@config`, `@context`, `@cross-borough`, `@distrib`, `@docs`, `@duration`, `@feed`, `@format`, `@glossary`, `@i18n`, `@integration`, `@logging`, `@manim`, `@model`, `@multilang`, `@no-yaml`, `@parallel`, `@pipeline`, `@post`, `@provenance`, `@rag`, `@scenario`, `@sequential`, `@strategy`, `@strict`, `@style`, `@subtitles`, `@timeout`, `@transcript`, `@translation`, `@tts`, `@validation`, `@versioning`, `@vtt`, `@wiring`। **38** समर्पित `cucumberTest*` टास्क प्रत्येक एक feature को अलग करते हैं (`build.gradle.kts` में `CucumberTaskSpec`)।

टेस्ट बेसलाइन: प्रामाणिक हरा रंग `main` पर CI `Full Tests` वर्कफ़्लो है
(यूनिट + कार्यात्मक TestKit + समग्र/वास्तविक-रेंडर Cucumber; नैदानिक `cucumberTest`
CR-10 से डिफ़ॉल्ट रूप से स्किप)।

## LLM सामग्री पाइपलाइन (CAP-ARCH-3)

`generateCapsuleContent` एक ही आह्वान में koog `CapsulePipelineGraph` का संचालन करता है:

```
propose-context → validate-context → generate-speaker-notes
```

- **इनपुट**: स्रोत डेक `.adoc` (`-Pdeck.file=<पथ>` या `slides/misc` में पहला `.adoc`),
  `collectCapsuleAugmentedContext` से वैकल्पिक संवर्धित संदर्भ
  (`build/capsule/augmented-context.txt`), लक्ष्य भाषा (`-Pdeck.language`, डिफ़ॉल्ट `fr`)।
- **आउटपुट** (घोषित → UP-TO-DATE के माध्यम से स्याही-अर्थव्यवस्था): संवर्धित वक्ता-नोट्स
  `build/capsule/<deckName>-speaker-notes.adoc` + TTS स्क्रिप्ट `build/capsule/<deckName>-script.txt`।
- **LLM**: codebase के `LlmBuildService` द्वारा हल; टेस्ट में mock Ollama सर्वर इंजेक्ट होता है
  `-Pollama.baseUrl=http://localhost:<port>` के साथ (प्रॉम्प्ट द्वारा रूट: propose → सामग्री योजना
  JSON, generate → संवर्धित AsciiDoc)। टेस्ट में शून्य नेटवर्क, शून्य पूल।

```bash
./gradlew generateCapsuleContent \
  -Pdeck.file=slides/misc/my-deck.adoc \
  -Pdeck.language=en
```

## JVM ट्यूनिंग

Cucumber टेस्ट `maxHeapSize = "1g"` और `forkEvery = 1` (प्रति परिदृश्य नया JVM)
तथा `maxParallelForks = 1` (स्थिरता हेतु क्रमिक निष्पादन) का उपयोग करते हैं।

```bash
export GRADLE_OPTS="-Xmx2g"   # स्थानीय बिल्ड
```

## बिल्ड कमांड

```bash
./gradlew build                       # पूर्ण बिल्ड (कंपाइल + टेस्ट)
./gradlew build -x test               # केवल कंपाइल
./gradlew check                       # सभी टेस्ट (यूनिट + Cucumber + कार्यात्मक)
./gradlew test                        # केवल JUnit5 यूनिट
./gradlew cucumberTest -PrunCucumber  # Cucumber BDD (समग्र रनर)
./gradlew functionalTest              # Gradle TestKit
./gradlew koverReport                 # कवरेज रिपोर्ट (XML + HTML)
./gradlew publishToMavenLocal         # स्थानीय प्रकाशन
```

## CI पाइपलाइन

`.github/workflows/` के अंतर्गत चार वर्कफ़्लो:

| वर्कफ़्लो | ट्रिगर | जॉब |
|-----------|--------|-----|
| `test.yml` — Fast Tests (PR गेट) | push / PR | JDK 25 + कैश्ड Gradle → `./gradlew test` (यूनिट) |
| `full-tests.yml` — Full Tests | push | ffmpeg + espeak, **कैश्ड Playwright Chromium**, कैश्ड Gradle → `./gradlew test functionalTest`, फिर `./gradlew cucumberTest cucumberTestViralRender -PrunCucumber` |
| `cucumber-full.yml` — नैदानिक | मैनुअल (`workflow_dispatch`) | पूर्ण `cucumberTest` (गैर-अवरोधक) |
| `llm-smoke.yml` — Ollama smoke | मैनुअल | कैश्ड Ollama इमेज + मॉडल, मॉडल के उत्तर देने की जाँच |

कैश: `gradle/actions/setup-gradle@v4` (wrapper + निर्भरताएँ + build cache),
`.github/actions/setup-playwright` (Chromium, कुंजी कैटलॉग की `playwright` संस्करण से),
`.github/actions/setup-ollama` (इमेज + मॉडल)।

## प्रकाशन (NMCP)

**Maven Central पर प्रकाशित** — संस्करण `0.0.10` (पूर्व `0.0.7`, `0.0.6`, `0.0.5`, …)।
POM `maven-publish` और `signing` द्वारा कॉन्फ़िगर है:

- POM: name "Capsule Gradle Plugin", description "Generation automatisee de capsules video
  depuis des decks reveal.js", URL `github.com/cheroliv/capsule-gradle/`
- हस्ताक्षर: `useGpgCmd()` (प्रकाशन पर निष्पादित)
- NMCP एकत्रीकरण: `publishAggregationToCentralPortal` (क्रेडेंशियल `~/.gradle/gradle.properties` में)

```bash
./gradlew publishToMavenLocal
./gradlew publishAggregationToCentralPortal --no-daemon
```

## आर्किटेक्चर व गवर्नेंस दस्तावेज़

- [doc/CAPSULE_ARCHITECTURE.adoc](../doc/CAPSULE_ARCHITECTURE.adoc) — पाइपलाइन आर्किटेक्चर (PlantUML)
- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPIC, रोडमैप व गवर्नेंस डैशबोर्ड
- [AGENT.adoc](../AGENT.adoc) — निरपेक्ष नियम (स्याही-अर्थव्यवस्था नियम सहित)
- [BACKLOG.adoc](../BACKLOG.adoc) — विस्तृत EPIC स्थिति

सभी EPIC बंद हैं: `CAP-VIRAL` (14/14), `CAP-SHORT` (6/6), `CAP-VIRAL-E2E` (3/3),
`CAP-CONTEXT` (4/4), `CAP-QC` (9/9), `CAP-QC-VOICE` (5/5), `LLM-RESILIENCE` (1/1),
`CAP-ARCH`, `CAP-29`, `CAP-MP4`, `CAP-AUDIO`, `CAP-TRANSCRIPT`, `CAP-PODCAST`,
`CAP-GLOSSARY`, `CAP-PROVENANCE`, `CAP-27`, `CAP-28`, `RENAME`, तथा CR-1→CR-10 समीक्षा-समूह।
कार्रवाई-योग्य बैकलॉग खाली है (देखें `.agents/INDEX.adoc`)।

## योगदान

1. बिल्ड कंपाइल हो: `./gradlew build -x test`
2. सभी टेस्ट हरे: `./gradlew check` (BDD हेतु `-PrunCucumber`)
3. निरपेक्ष नियमों का पालन करें (देखें [AGENT.adoc](../AGENT.adoc)):
   - अनुमति के बिना कोई commit/push नहीं
   - DAG N2 — N3 द्वारा आयातनीय, कभी N3 आयात नहीं
   - Capsule slider का उपभोग करता है (केवल-पठन, स्रोत डेक कभी नहीं बदलता)
   - कोड में शून्य रहस्य/टोकन
4. स्याही-अर्थव्यवस्था नियम का पालन करें — जब उसी इनपुट हेतु वैध आर्टिफ़ैक्ट पहले से मौजूद हो
   तो महँगा ऑपरेशन (TTS, Playwright कैप्चर) कभी दोबारा न चलाएँ
5. Baby-step DDD/TDD/BDD — एक बार में एक फ़ाइल; यूनिट → कार्यात्मक → Cucumber

## लाइसेंस

Apache License 2.0 — देखें [LICENSE](../../LICENSE).

---

_CCCP Education पारिस्थितिकी तंत्र का भाग — `groupId: education.cccp`।_
