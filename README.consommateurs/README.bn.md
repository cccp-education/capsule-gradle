<!-- translated from README.md rev 0.0.8 -->
# capsule-gradle — ব্যবহারকারী নির্দেশিকা

> একটি Gradle প্লাগইন যা reveal.js ডেককে সিঙ্ক্রোনাইজড ভিডিও ক্যাপসুলে রূপান্তর করে — TTS ভয়েস-ওভার, ক্যাপচার, সাবটাইটেল, ফরম্যাট রূপান্তর, অডিও পোস্ট এবং একটি শর্ট-ফর্ম/ভাইরাল পাইপলাইন — কোনো মানব পোস্ট-প্রোডাকশন ছাড়াই।

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.8&color=green)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![লাইসেন্স](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=%E0%A6%B2%E0%A6%BE%E0%A6%87%E0%A6%B8%E0%A7%87%E0%A6%A8%E0%A7%8D%E0%A6%B8)](../LICENSE)

- **সংস্করণ**: `0.0.8` (Maven Central) · **গ্রুপ**: `education.cccp` · **প্লাগইন ID**: `education.cccp.capsule`
- **আবশ্যক**: Java 25+ · Gradle 9.8+
- **ডকস**: [প্লাগইন অভ্যন্তরীণ](../capsule-plugin/README.md) · [পাইপলাইন আর্কিটেকচার](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc)

🌐 ভাষা: [English](../README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | **বাংলা** | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## এটি কী করে

`capsule-gradle` একটি reveal.js ডেক (`slider-gradle` দ্বারা উৎপাদিত) ভোগ করে এবং একটি
সিঙ্ক্রোনাইজড ভিডিও ক্যাপসুল তৈরি করে — প্রতি স্লাইডে একটি TTS ভয়েস-ওভার, ভিডিও ক্যাপচার,
সাবটাইটেল, ফরম্যাট রূপান্তর, অডিও পোস্ট-প্রোডাকশন, গুণমান নিয়ন্ত্রণ এবং একটি ঐচ্ছিক
ভার্টিকাল শর্ট-ফর্ম (ভাইরাল) পাইপলাইন।

```
AsciiDoc উৎস → slider-gradle → reveal.js ডেক + বক্তা স্ক্রিপ্ট
                                        ↓
        capsule-gradle → TTS (Piper/espeak) → অডিও ইনজেকশন → ক্যাপচার → WebM/MP4
                                        ↓                        ↓
                              Manim অ্যানিমেশন (ঐচ্ছিক)   FFmpeg দিয়ে 9:16 রিক্রপ (ঐচ্ছিক)
```

CCCP Education বহু-প্লাগইন ইকোসিস্টেমের অংশ (`groupId: education.cccp`)।

## আবশ্যকতা

- **Java** 25+ (Kotlin 2.4.10 টুলচেইন)
- **Gradle** 9.8+
- **Piper** — স্থানীয় অফলাইন TTS (ডিফল্ট ইঞ্জিন)
- **espeak / espeak-ng** — TTS ফলব্যাক (`espeak` অনুপস্থিত থাকলে `espeak-ng` স্বয়ংক্রিয়ভাবে ব্যবহৃত হয়)
- **Chromium** — প্রথম রানে Playwright দ্বারা স্বয়ংক্রিয় ডাউনলোড
- **FFmpeg** — 9:16 রিক্রপ, সাবটাইটেল burn-in, MP4 রূপান্তর, অডিও পোস্ট
- **Manim** — ঐচ্ছিক, অ্যানিমেটেড গাণিতিক স্লাইডের জন্য
- **Node** — ঐচ্ছিক, Remotion ক্যাপচার/অ্যানিমেশন কৌশলের জন্য

## দ্রুত শুরু

### ১. প্লাগইন প্রয়োগ করুন

```gradle
plugins {
    id("education.cccp.capsule") version "0.0.8"
}
```

### ২. কনফিগার করুন

```gradle
capsule {
    outputDir.set("capsules")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    ttsEngine.set("espeak")   // "piper" (ডিফল্ট) বা "espeak"
}
```

### ৩. ভিডিও তৈরি করুন

```bash
./gradlew generateCapsuleVideo
```

এটি সম্পূর্ণ পাইপলাইন চালায়: স্ক্রিপ্ট যাচাই → TTS সংশ্লেষ → ডেক HTML-এ অডিও ইনজেকশন →
হেডলেস ক্যাপচার → ভিডিও আউটপুট।

### slider-এর সাথে সম্পূর্ণ পাইপলাইন

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
./gradlew generateCapsuleVideo    # slider ডেক + স্ক্রিপ্ট → ভিডিও ক্যাপসুল
```

## উপলব্ধ টাস্ক

| টাস্ক | গ্রুপ | বিবরণ |
|-------|-------|--------|
| `generateCapsuleScript` | generate | slider-নির্মিত `*-script.txt` পড়ে ও ক্যাপসুল স্ক্রিপ্ট যাচাই করে |
| `generateCapsule` | generate | TTS অডিও ফাইল (Piper/espeak) সংশ্লেষ করে, প্রতি স্লাইডে একটি |
| `generateCapsuleVideo` | generate | ডেক HTML-এ TTS অডিও ইনজেক্ট করে, তারপর ভিডিও ক্যাপচার করে |
| `generateCapsuleVideoAllLanguages` | generate | প্রতি লক্ষ্য ভাষায় একটি স্থানীয়কৃত ক্যাপসুল (`LanguageCatalog`) |
| `extractSpeakerNotes` | capsule | AsciiDoc বক্তা-নোটকে প্লেইন-টেক্সট `*-script.txt` চুক্তিতে পার্স করে |
| `translateAndExtractSpeakerNotes` | capsule | যৌগিক: `translateDeck` (slider) → `extractSpeakerNotes` |
| `translateAndGenerateCapsuleVideos` | capsule | যৌগিক: `translateDeck` → `extractSpeakerNotes` → `generateCapsuleVideoAllLanguages` |
| `generateCapsuleContent` | generate | LLM পাইপলাইন (koog): propose-context → যাচাই → বক্তা-নোট + TTS স্ক্রিপ্ট |
| `generateCapsuleContentAndVideos` | capsule | যৌগিক: `generateCapsuleContent` → `translateAndGenerateCapsuleVideos` |
| `collectCapsuleAugmentedContext` | collect | সমৃদ্ধ প্রসঙ্গ সংগ্রহ করে (গভর্নেন্স + RAG + Docs + দৃশ্যকল্প + শব্দকোষ) |
| `capsuleAiSmokeTest` | generate | সর্বনিম্ন প্রম্পট দিয়ে codebase LLM সেতুর স্মোক টেস্ট |
| `generateCapsuleTranscript` | generate | বক্তা-নোট থেকে AsciiDoc ট্রান্সক্রিপ্ট নিবন্ধ তৈরি করে |
| `generateCapsuleChapters` | generate | অধ্যায় মার্কার + YouTube টেক্সট অধ্যায় + বিবরণ |
| `generateCapsulePodcast` | generate | কেবল-অডিও পডকাস্ট (সংযুক্ত বর্ণনা MP3) |
| `deployCapsule` | deploy | FFmpeg দিয়ে ক্যাপসুলকে ভার্টিকাল 9:16 (TikTok/Shorts) রিক্রপ করে |
| `distributeCapsuleVideo` | distribute | চূড়ান্ত ভিডিও সংস্করণযুক্ত গন্তব্য ডিরেক্টরিতে কপি করে |
| `collectCapsuleContext` / `transformCapsuleContext` / `collectCapsuleRetrieve` | collect/transform | ক্যাপসুল প্রসঙ্গ JSON রপ্তানি / পার্স করে (N3 ইঞ্জিন চুক্তি) |
| `scaffoldCapsuleContext` | generate | মন্তব্যযুক্ত ডিফল্ট `capsule-context.yml` তৈরি করে |
| `validateCapsuleVideoDuration` | verification | ভিডিও দৈর্ঘ্যকে TTS অডিও দৈর্ঘ্যের যোগফলের সাথে মেলায় (opt-in) |
| `preflightCapsuleContent` | verification | সশুল্ক TTS-এর আগে বিষয়বস্তু প্রি-ফ্লাইট (opt-in) |
| `renderQcBoards` | verification | QC বোর্ড (beats + «বলা vs দেখানো») রেন্ডার করে (opt-in) |
| `verifyCapsuleVoice` | verification | ASR দিয়ে ভয়েস QC — নিষ্ঠা, শ্বাস, টেক নির্বাচন (opt-in) |
| `selectBestVoiceTake` | verification | একটি স্লাইডের প্রার্থীদের মধ্যে সেরা টেক নির্বাচন করে (opt-in) |
| `generateViralCampaign` | generate | Storyboard → শর্ট ক্যাম্পেইন (YouTube/TikTok) (opt-in) |
| `renderViralVariants` / `renderViralThumbnails` / `renderViralSubtitles` | generate | ভাইরাল ভ্যারিয়েন্ট, থাম্বনেইল, কারাওকে সাবটাইটেল রেন্ডার করে (opt-in) |
| `validateViralCampaign` | verification | উৎপন্ন ফাইলের বিপরীতে ম্যানিফেস্ট যাচাই করে (opt-in) |
| `normalizeViralStoryboard` | generate | storyboard-এর অনুপস্থিত ট্রানজিশন পূরণ করে (লেখা সহায়ক) |

> ঐচ্ছিক (*opt-in*) ক্ষমতাগুলি **ডিফল্টে নিষ্ক্রিয়** (`false`) এবং মূল পাইপলাইনকে কখনও
> অবরুদ্ধ করে না।

## কনফিগারেশন

প্রতিটি মান **৪টি উৎস** থেকে নির্ধারিত হয় (অগ্রাধিকার **CLI > YAML > gradle.properties > ENV**):

```bash
./gradlew scaffoldCapsuleContext          # মন্তব্যযুক্ত capsule-context.yml তৈরি করে
./gradlew generateCapsuleVideo -Pcapsule.tts.engine=espeak -Pcapsule.manim.quality=h
```

প্রতিনিধি DSL (সম্পূর্ণ 18-বিভাগীয় `CapsuleConfig`-এর জন্য [অভ্যন্তরীণ নির্দেশিকা](../capsule-plugin/README.md) দেখুন):

```gradle
capsule {
    // ইনপুট
    outputDir.set("capsule")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")

    // TTS
    ttsEngine.set("piper")                 // "piper" (ডিফল্ট) বা "espeak"
    ttsVoice.set("fr_FR-siwis-medium")
    piperExecutablePath.set("piper")
    ttsFallbackEnabled.set(true)
    espeakVoice.set("fr")
    espeakSpeed.set(150)
    ttsLanguage.set("fr")                  // ২২ ভাষা (LanguageCatalog)

    // ক্যাপচার / আউটপুট
    viewportWidth.set(1408)
    viewportHeight.set(792)
    captureStrategy.set("playwright")      // "playwright" | "screenshot" | "remotion"
    outputFormat.set("webm")               // "webm" (ডিফল্ট) | "mp4" | "both"
    outputBt709.set(false)                 // MP4-কে BT.709 হিসেবে ট্যাগ করুন (opt-in)

    // সাবটাইটেল
    subtitleEnabled.set(false)
    subtitleFormat.set("srt")              // "srt" বা "vtt"
    subtitleBurnIn.set(false)

    // বিতরণ
    ffmpegExecutablePath.set("ffmpeg")
    distribOutputWidth.set(1080)
    distribOutputHeight.set(1920)

    // Manim (ঐচ্ছিক)
    manimExecutablePath.set("manim")
    manimQuality.set("l")                  // l, m, h, p, k
}
```

## বহু-ভাষা পাইপলাইন

একটি উৎস ডেক → প্রতি লক্ষ্য ভাষায় একটি স্থানীয়কৃত ক্যাপসুল। পাইপলাইন `slider.translateDeck`
দ্বারা উৎপাদিত অনূদিত ডেক ভোগ করে (**২২ ভাষা**, N0 `i18n-contracts`-এর `LanguageCatalog`),
এবং প্রতি-ভাষা TTS ভয়েস বিশুদ্ধ ডোমেইন `capsule.multilang` (`MultiLanguageResolver` +
`VoiceMapping`) দিয়ে নির্ধারণ করে।

```
উৎস ডেক (AsciiDoc)
        ↓ slider.translateDeck (LanguageCatalog, ২২ ভাষা)
<deckName>_<lang>-deck.html (প্রতি ভাষায় একটি)
        ↓ capsule.extractSpeakerNotes
<deckName>_<lang>-script.txt (প্রতি ভাষায় একটি)
        ↓ capsule.generateCapsuleVideoAllLanguages
<deckName>_<lang>.webm (প্রতি ভাষায় একটি স্থানীয়কৃত ক্যাপসুল)
```

নামকরণ প্রথা (সীমানা চুক্তি, slider-এর সাথে সংগতিপূর্ণ):

- অনূদিত ডেক: `<deckName>_<lang>-deck.html`
- বক্তা স্ক্রিপ্ট: `<deckName>_<lang>-script.txt`
- স্থানীয়কৃত ভিডিও: `<deckName>_<lang>.webm`

```bash
./gradlew generateCapsuleVideoAllLanguages   # ডেক+স্ক্রিপ্ট জোড়া থাকা প্রতিটি ভাষায় একটি ভিডিও
./gradlew translateAndGenerateCapsuleVideos  # সম্পূর্ণ যৌগিক: translate → extract → ভিডিও
```

মিলে যাওয়া ডেক + স্ক্রিপ্ট জোড়া ছাড়া ভাষা নীরবে এড়িয়ে যাওয়া হয়, এবং
**কালি-সাশ্রয়** নিয়ম প্রযোজ্য: যে ভাষার ভিডিও আগে থেকেই আছে ও বৈধ, তা কখনও পুনরায়
রেন্ডার হয় না।

## ঐচ্ছিক সুবিধা

- **আউটপুট ফরম্যাট** — `outputFormat = "mp4"` (বা `"both"`) নেটিভ WebM-এর পাশাপাশি
  MP4/H.264 তৈরি করে; `outputBt709 = true` এটিকে BT.709 হিসেবে ট্যাগ করে (ব্রডকাস্ট রং)।
- **অডিও পোস্ট-প্রোডাকশন** — পটভূমি সংগীত, EBU R128 লাউডনেস (২-পাস পরিমাপ) এবং sidechain
  ducking (`audioPost { … }`)।
- **সাবটাইটেল** — SRT/VTT তৈরি ও burn-in (`subtitleEnabled`, `subtitleBurnIn`)।
- **গুণমান নিয়ন্ত্রণ (opt-in)** — বিষয়বস্তু প্রি-ফ্লাইট (`preflightCapsuleContent`),
  ডিকোডিং দিয়ে ভিডিও অখণ্ডতা, নিয়তবাদী timeline, QC বোর্ড (`renderQcBoards`) এবং ASR দিয়ে
  ভয়েস QC (`verifyCapsuleVoice`, `selectBestVoiceTake`)।
- **Transcript** — উল্টো পাইপলাইন: বক্তা-নোট থেকে AsciiDoc নিবন্ধ (`TEMPLATE` বা `LLM`)।
- **Podcast** — কেবল-অডিও বর্ণনা MP3।
- **Preview** — dry-run (`previewOnly = true`): কেবল স্লাইড PNG রেন্ডার করে, mux বাদ দেয়।
- **ভাইরাল (opt-in)** — যাচাইযোগ্য storyboard → সমৃদ্ধ প্রসঙ্গে নোঙর করা hooks/CTA সহ
  ভার্টিকাল 9:16 শর্ট ক্যাম্পেইন, থাম্বনেইল, কারাওকে সাবটাইটেল এবং `CampaignBundle`।

### স্লাইডের ছন্দ (Remotion ক্যাপচার)

`captureStrategy = "remotion"`-এর সাথে, ডেক চিত্রায়িত হওয়ার বদলে অ্যানিমেট হয়: স্লাইডের
প্রতিটি ব্লক পুরো স্লাইড একসাথে আসার বদলে পড়ার ছন্দে আসে। ডিফল্ট beats (স্লাইডের শুরু থেকে
সেকেন্ড): `.kicker` 0.0 · `h1`/`h2` 0.15 · `.rule` 0.5 · `.lede` 1.0 · `.stat` 4.5 ·
`.statlabel` 5.0। `.brand`, `.num` এবং `.grid` হলো chrome (প্রথম ফ্রেম থেকেই উপস্থিত)।
যেকোনো ব্লক `data-at` দিয়ে নিজের beat সেট করতে পারে:

```html
<p class="stat" data-at="8.5">542 076 km</p>
```

## বিল্ড ও টেস্ট (অবদানকারী)

```bash
./gradlew build                    # সম্পূর্ণ বিল্ড
./gradlew check                    # ইউনিট + ফাংশনাল + Cucumber
./gradlew test                     # কেবল JUnit5 ইউনিট
./gradlew cucumberTest -PrunCucumber
./gradlew functionalTest           # Gradle TestKit
./gradlew publishToMavenLocal      # স্থানীয় প্রকাশ
```

## সমস্যা সমাধান

| লক্ষণ | সমাধান |
|-------|--------|
| Playwright Chromium পাওয়া যায়নি | একবার চালিয়ে স্বয়ংক্রিয় ডাউনলোড করুন, বা `chromiumExecutablePath` সেট করুন |
| Piper TTS কোনো অডিও দেয় না | `piperExecutablePath` দেখুন, ভয়েস মডেল ইনস্টল আছে নিশ্চিত করুন |
| `deployCapsule` ব্যর্থ | FFmpeg `PATH`-এ আছে কিনা যাচাই করুন (`ffmpeg -version`) |
| Manim স্লাইড রেন্ডার হয়নি | Manim ছাড়া পরীক্ষার জন্য `manimExecutablePath` = `noop` সেট করুন |
| ভিডিও ক্যাপচার টাইমআউট | `playwrightTimeout` বাড়ান (ডিফল্ট 120000 ms) |
| অপর্যাপ্ত মেমরি | `export GRADLE_OPTS="-Xmx2g"` |

## ডকুমেন্টেশন

- [capsule-plugin/README.md](../capsule-plugin/README.md) — প্লাগইন অভ্যন্তরীণ (ডেভেলপার ও অবদানকারী নির্দেশিকা)
- [capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) — পাইপলাইন আর্কিটেকচার (PlantUML)

## লাইসেন্স

Apache License 2.0 — দেখুন [LICENSE](../LICENSE).

---

_CCCP Education ইকোসিস্টেমের অংশ — `groupId: education.cccp`।_
