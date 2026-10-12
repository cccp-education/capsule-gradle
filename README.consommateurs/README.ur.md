<!-- translated from README.md rev 0.0.10 -->
# capsule-gradle — صارف رہنمائی

> ایک Gradle پلگ ان جو reveal.js ڈیک کو ہم آہنگ ویڈیو کیپسول میں بدلتا ہے — TTS وائس اوور، کیپچر، سب ٹائٹلز، فارمیٹ تبدیلی، آڈیو پوسٹ اور ایک مختصر/وائرل پائپ لائن — بغیر کسی انسانی پوسٹ پروڈکشن کے۔

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.10&color=green)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![لائسنس](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=%D9%84%D8%A7%D8%A6%D8%B3%D9%86%D8%B3)](../LICENSE)

- **ورژن**: `0.0.10` (Maven Central) · **گروپ**: `education.cccp` · **پلگ ان ID**: `education.cccp.capsule`
- **درکار**: Java 25+ · Gradle 9.8+
- **دستاویزات**: [پلگ ان کے اندرونی معاملات](../capsule-plugin/README.md) · [پائپ لائن آرکیٹیکچر](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc)

🌐 زبانیں: [English](../README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | **اردو**

---

## یہ کیا کرتا ہے

`capsule-gradle` ایک reveal.js ڈیک (جو `slider-gradle` بناتا ہے) استعمال کرتا ہے اور ایک
ہم آہنگ ویڈیو کیپسول بناتا ہے — ہر سلائیڈ کے لیے ایک TTS وائس اوور، ویڈیو کیپچر، سب ٹائٹلز،
فارمیٹ تبدیلی، آڈیو پوسٹ پروڈکشن، معیار کی جانچ اور ایک اختیاری عمودی مختصر (وائرل) پائپ لائن۔

```
AsciiDoc ماخذ → slider-gradle → reveal.js ڈیک + اسپیکر اسکرپٹ
                                        ↓
        capsule-gradle → TTS (Piper/espeak) → آڈیو انجیکشن → کیپچر → WebM/MP4
                                        ↓                        ↓
                              Manim اینیمیشن (اختیاری)   FFmpeg سے 9:16 دوبارہ کراپ (اختیاری)
```

CCCP Education کثیر پلگ ان ماحولیاتی نظام کا حصہ (`groupId: education.cccp`)۔

## تقاضے

- **Java** 25+ (Kotlin 2.4.10 ٹول چین)
- **Gradle** 9.8+
- **Piper** — مقامی آف لائن TTS (طے شدہ انجن)
- **espeak / espeak-ng** — TTS فال بیک (`espeak` نہ ہونے پر `espeak-ng` خودکار استعمال ہوتا ہے)
- **Fish Audio** — اختیاری کلاؤڈ TTS، **کلون شدہ آواز** کے ساتھ (انجن `fish`)؛ کلیدوں کا پول git سے مستثنیٰ `fish.yaml` میں رہتا ہے
- **Chromium** — پہلی بار چلانے پر Playwright خودکار ڈاؤن لوڈ کرتا ہے
- **FFmpeg** — 9:16 دوبارہ کراپ، سب ٹائٹل burn-in، MP4 تبدیلی، آڈیو پوسٹ
- **Manim** — اختیاری، متحرک ریاضیاتی سلائیڈوں کے لیے
- **Node** — اختیاری، Remotion کیپچر/اینیمیشن حکمتِ عملی کے لیے

## تیز آغاز

### ۱۔ پلگ ان لگائیں

```gradle
plugins {
    id("education.cccp.capsule") version "0.0.10"
}
```

### ۲۔ ترتیب دیں

```gradle
capsule {
    outputDir.set("capsules")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    ttsEngine.set("espeak")   // "piper" (طے شدہ), "espeak" یا "fish" (Fish Audio کلاؤڈ، کلون شدہ آواز)
}
```

### ۳۔ ویڈیو بنائیں

```bash
./gradlew generateCapsuleVideo
```

یہ پوری پائپ لائن چلاتا ہے: اسکرپٹ کی تصدیق → TTS ترکیب → ڈیک HTML میں آڈیو انجیکشن →
ہیڈ لیس کیپچر → ویڈیو آؤٹ پٹ۔

### slider کے ساتھ مکمل پائپ لائن

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
./gradlew generateCapsuleVideo    # slider ڈیک + اسکرپٹ → ویڈیو کیپسول
```

## دستیاب ٹاسک

| ٹاسک | گروپ | تفصیل |
|------|------|-------|
| `generateCapsuleScript` | generate | slider کے بنائے `*-script.txt` کو پڑھتا اور کیپسول اسکرپٹ کی تصدیق کرتا ہے |
| `generateCapsule` | generate | TTS آڈیو فائلیں (Piper/espeak) ترکیب کرتا ہے، ہر سلائیڈ کے لیے ایک |
| `generateCapsuleVideo` | generate | ڈیک HTML میں TTS آڈیو انجیکٹ کرتا ہے، پھر ویڈیو کیپچر کرتا ہے |
| `generateCapsuleVideoAllLanguages` | generate | ہر ہدف زبان کے لیے ایک مقامی کیپسول (`LanguageCatalog`) |
| `extractSpeakerNotes` | capsule | AsciiDoc اسپیکر نوٹس کو سادہ متن `*-script.txt` معاہدے میں پارس کرتا ہے |
| `translateAndExtractSpeakerNotes` | capsule | مرکب: `translateDeck` (slider) → `extractSpeakerNotes` |
| `translateAndGenerateCapsuleVideos` | capsule | مرکب: `translateDeck` → `extractSpeakerNotes` → `generateCapsuleVideoAllLanguages` |
| `generateCapsuleContent` | generate | LLM پائپ لائن (koog): propose-context → تصدیق → اسپیکر نوٹس + TTS اسکرپٹ |
| `generateCapsuleContentAndVideos` | capsule | مرکب: `generateCapsuleContent` → `translateAndGenerateCapsuleVideos` |
| `collectCapsuleAugmentedContext` | collect | افزودہ سیاق جمع کرتا ہے (گورننس + RAG + Docs + منظرنامہ + فرہنگ) |
| `capsuleAiSmokeTest` | generate | کم سے کم پرامپٹ سے codebase LLM پُل کا اسموک ٹیسٹ |
| `generateCapsuleTranscript` | generate | اسپیکر نوٹس سے AsciiDoc ٹرانسکرپٹ مضمون بناتا ہے |
| `generateCapsuleChapters` | generate | ابواب کے نشانات + YouTube متنی ابواب + تفصیل |
| `generateCapsulePodcast` | generate | صرف آڈیو پوڈکاسٹ (جوڑی گئی بیانیہ MP3) |
| `deployCapsule` | deploy | FFmpeg سے کیپسول کو عمودی 9:16 (TikTok/Shorts) میں دوبارہ کراپ کرتا ہے |
| `distributeCapsuleVideo` | distribute | حتمی ویڈیوز کو ورژن شدہ منزل والے فولڈر میں نقل کرتا ہے |
| `collectCapsuleContext` / `transformCapsuleContext` / `collectCapsuleRetrieve` | collect/transform | کیپسول سیاق JSON برآمد / پارس کرتا ہے (N3 انجن معاہدہ) |
| `scaffoldCapsuleContext` | generate | تبصرے والی طے شدہ `capsule-context.yml` بناتا ہے |
| `validateCapsuleVideoDuration` | verification | ویڈیو دورانیے کو TTS آڈیو دورانیوں کے مجموعے سے ملاتا ہے (opt-in) |
| `preflightCapsuleContent` | verification | ادا شدہ TTS سے پہلے مواد کی پیشگی جانچ (opt-in) |
| `renderQcBoards` | verification | QC بورڈز (beats + «کہا بمقابلہ دکھایا») بناتا ہے (opt-in) |
| `verifyCapsuleVoice` | verification | ASR کے ذریعے وائس QC — نزاکت، سانس، ٹیک کا انتخاب (opt-in) |
| `selectBestVoiceTake` | verification | سلائیڈ کے امیدواروں میں سے بہترین ٹیک منتخب کرتا ہے (opt-in) |
| `generateViralCampaign` | generate | Storyboard → مختصر مہم (YouTube/TikTok) (opt-in) |
| `renderViralVariants` / `renderViralThumbnails` / `renderViralSubtitles` | generate | وائرل اقسام، تھمب نیل، کیراوکے سب ٹائٹلز بناتا ہے (opt-in) |
| `validateViralCampaign` | verification | پیدا شدہ فائلوں کے مقابل مینی فیسٹ کی جانچ کرتا ہے (opt-in) |
| `normalizeViralStoryboard` | generate | storyboard کے غائب ٹرانزیشن مکمل کرتا ہے (تصنیف معاون) |

> اختیاری (*opt-in*) صلاحیتیں **طے شدہ طور پر معطل** (`false`) ہیں اور بنیادی پائپ لائن کو
> کبھی معطل نہیں کرتیں۔

## ترتیب

ہر قدر **4 ذرائع** سے حل ہوتی ہے (ترجیح **CLI > YAML > gradle.properties > ENV**):

```bash
./gradlew scaffoldCapsuleContext          # تبصرے والی capsule-context.yml بناتا ہے
./gradlew generateCapsuleVideo -Pcapsule.tts.engine=espeak -Pcapsule.manim.quality=h
```

نمائندہ DSL (مکمل 18-حصوں والے `CapsuleConfig` کے لیے [اندرونی رہنمائی](../capsule-plugin/README.md) دیکھیں):

```gradle
capsule {
    // ان پٹ
    outputDir.set("capsule")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")

    // TTS
    ttsEngine.set("piper")                 // "piper" (طے شدہ) یا "espeak"
    ttsVoice.set("fr_FR-siwis-medium")
    piperExecutablePath.set("piper")
    ttsFallbackEnabled.set(true)
    espeakVoice.set("fr")
    espeakSpeed.set(150)
    ttsLanguage.set("fr")                  // 22 زبانیں (LanguageCatalog)

    // کیپچر / آؤٹ پٹ
    viewportWidth.set(1408)
    viewportHeight.set(792)
    captureStrategy.set("playwright")      // "playwright" | "screenshot" | "remotion"
    outputFormat.set("webm")               // "webm" (طے شدہ) | "mp4" | "both"
    outputBt709.set(false)                 // MP4 کو BT.709 کے طور پر ٹیگ کریں (opt-in)

    // سب ٹائٹلز
    subtitleEnabled.set(false)
    subtitleFormat.set("srt")              // "srt" یا "vtt"
    subtitleBurnIn.set(false)

    // تقسیم
    ffmpegExecutablePath.set("ffmpeg")
    distribOutputWidth.set(1080)
    distribOutputHeight.set(1920)

    // Manim (اختیاری)
    manimExecutablePath.set("manim")
    manimQuality.set("l")                  // l, m, h, p, k
}
```

## کثیر لسانی پائپ لائن

ایک ماخذ ڈیک → ہر ہدف زبان کے لیے ایک مقامی کیپسول۔ پائپ لائن `slider.translateDeck` کے
بنائے ہوئے ترجمہ شدہ ڈیک استعمال کرتی ہے (**22 زبانیں**، N0 `i18n-contracts` کی
`LanguageCatalog`)، اور ہر زبان کی TTS آواز خالص ڈومین `capsule.multilang`
(`MultiLanguageResolver` + `VoiceMapping`) سے حل کرتی ہے۔

```
ماخذ ڈیک (AsciiDoc)
        ↓ slider.translateDeck (LanguageCatalog، 22 زبانیں)
<deckName>_<lang>-deck.html (ہر زبان کے لیے ایک)
        ↓ capsule.extractSpeakerNotes
<deckName>_<lang>-script.txt (ہر زبان کے لیے ایک)
        ↓ capsule.generateCapsuleVideoAllLanguages
<deckName>_<lang>.webm (ہر زبان کے لیے ایک مقامی کیپسول)
```

نام رکھنے کا اصول (حدّی معاہدہ، slider کے ساتھ ہم آہنگ):

- ترجمہ شدہ ڈیک: `<deckName>_<lang>-deck.html`
- اسپیکر اسکرپٹ: `<deckName>_<lang>-script.txt`
- مقامی ویڈیو: `<deckName>_<lang>.webm`

```bash
./gradlew generateCapsuleVideoAllLanguages   # ڈیک+اسکرپٹ جوڑے والی ہر زبان کے لیے ایک ویڈیو
./gradlew translateAndGenerateCapsuleVideos  # مکمل مرکب: translate → extract → ویڈیوز
```

جو زبان ڈیک + اسکرپٹ کے مماثل جوڑے کے بغیر ہو، خاموشی سے نظرانداز ہو جاتی ہے، اور
**سیاہی کی معیشت** کا اصول لاگو ہوتا ہے: جس زبان کی ویڈیو پہلے سے موجود اور درست ہو، وہ
کبھی دوبارہ رینڈر نہیں ہوتی۔

## اختیاری خصوصیات

- **آؤٹ پٹ فارمیٹ** — `outputFormat = "mp4"` (یا `"both"`) مقامی WebM کے علاوہ MP4/H.264
  بناتا ہے؛ `outputBt709 = true` اسے BT.709 کے طور پر ٹیگ کرتا ہے (نشریاتی رنگت)۔
- **آڈیو پوسٹ پروڈکشن** — پس منظر موسیقی، EBU R128 بلندی (2-پاس پیمائش) اور sidechain
  ducking (`audioPost { … }`)۔
- **سب ٹائٹلز** — SRT/VTT تیاری اور burn-in (`subtitleEnabled`، `subtitleBurnIn`)۔
- **معیار کی جانچ (opt-in)** — مواد کی پیشگی جانچ (`preflightCapsuleContent`)، ڈی کوڈنگ سے
  ویڈیو سالمیت، قطعی timeline، QC بورڈز (`renderQcBoards`) اور ASR سے وائس QC
  (`verifyCapsuleVoice`، `selectBestVoiceTake`)۔
- **Transcript** — اُلٹی پائپ لائن: اسپیکر نوٹس سے AsciiDoc مضمون (`TEMPLATE` یا `LLM`)۔
- **Podcast** — صرف آڈیو بیانیہ MP3۔
- **Preview** — dry-run (`previewOnly = true`): صرف سلائیڈ PNG رینڈر کرتا ہے، mux چھوڑ دیتا ہے۔
- **وائرل (opt-in)** — قابلِ تصدیق storyboard → افزودہ سیاق پر لنگرانداز hooks/CTA کے ساتھ
  عمودی 9:16 مختصر مہم، تھمب نیل، کیراوکے سب ٹائٹلز اور `CampaignBundle`۔

### سلائیڈوں کی تال (Remotion کیپچر)

`captureStrategy = "remotion"` کے ساتھ، ڈیک فلمائے جانے کے بجائے اینیمیٹ ہوتا ہے: سلائیڈ کا
ہر بلاک پوری سلائیڈ کے ایک ساتھ آنے کے بجائے پڑھنے کی تال میں آتا ہے۔ طے شدہ beats (سلائیڈ
کے آغاز سے سیکنڈ): `.kicker` 0.0 · `h1`/`h2` 0.15 · `.rule` 0.5 · `.lede` 1.0 · `.stat` 4.5 ·
`.statlabel` 5.0۔ `.brand`، `.num` اور `.grid` chrome ہیں (پہلے فریم سے موجود)۔ کوئی بھی بلاک
`data-at` سے اپنا beat مقرر کر سکتا ہے:

```html
<p class="stat" data-at="8.5">542 076 km</p>
```

## بلڈ اور ٹیسٹ (معاونین)

```bash
./gradlew build                    # مکمل بلڈ
./gradlew check                    # یونٹ + فنکشنل + Cucumber
./gradlew test                     # صرف JUnit5 یونٹ
./gradlew cucumberTest -PrunCucumber
./gradlew functionalTest           # Gradle TestKit
./gradlew publishToMavenLocal      # مقامی اشاعت
```

## مسائل کا حل

| علامت | حل |
|-------|-----|
| Playwright Chromium نہیں ملا | ایک بار چلا کر خودکار ڈاؤن لوڈ کریں، یا `chromiumExecutablePath` مقرر کریں |
| Piper TTS آڈیو نہیں دیتا | `piperExecutablePath` دیکھیں، یقینی بنائیں کہ آواز کا ماڈل نصب ہے |
| `deployCapsule` ناکام | تصدیق کریں کہ FFmpeg `PATH` میں ہے (`ffmpeg -version`) |
| Manim سلائیڈز رینڈر نہیں ہوئیں | Manim کے بغیر ٹیسٹ کے لیے `manimExecutablePath` = `noop` مقرر کریں |
| ویڈیو کیپچر ٹائم آؤٹ | `playwrightTimeout` بڑھائیں (طے شدہ 120000 ms) |
| میموری ناکافی | `export GRADLE_OPTS="-Xmx2g"` |

## دستاویزات

- [capsule-plugin/README.md](../capsule-plugin/README.md) — پلگ ان کے اندرونی معاملات (ڈیولپر و معاون رہنمائی)
- [capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) — پائپ لائن آرکیٹیکچر (PlantUML)

## لائسنس

Apache License 2.0 — دیکھیں [LICENSE](../LICENSE).

---

_CCCP Education ماحولیاتی نظام کا حصہ — `groupId: education.cccp`._
