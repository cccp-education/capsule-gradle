<!-- translated from README.md rev 0.0.8 -->
# capsule-gradle — उपभोक्ता मार्गदर्शिका

> एक Gradle प्लगइन जो reveal.js डेक को सिंक्रनाइज़्ड वीडियो कैप्सूल में बदलता है — TTS वॉइस-ओवर, कैप्चर, सबटाइटल, फ़ॉर्मैट रूपांतरण, ऑडियो पोस्ट और एक शॉर्ट-फ़ॉर्म/वायरल पाइपलाइन — बिना मानवीय पोस्ट-प्रोडक्शन के।

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.8&color=green)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![लाइसेंस](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=%E0%A4%B2%E0%A4%BE%E0%A4%87%E0%A4%B8%E0%A5%87%E0%A4%82%E0%A4%B8)](../LICENSE)

- **संस्करण**: `0.0.8` (Maven Central) · **समूह**: `education.cccp` · **प्लगइन ID**: `education.cccp.capsule`
- **आवश्यक**: Java 25+ · Gradle 9.8+
- **दस्तावेज़**: [प्लगइन आंतरिक](../capsule-plugin/README.md) · [पाइपलाइन आर्किटेक्चर](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc)

🌐 भाषाएँ: [English](../README.md) | [中文](README.zh.md) | **हिन्दी** | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## यह क्या करता है

`capsule-gradle` एक reveal.js डेक (जो `slider-gradle` बनाता है) को उपभोग करता है और एक
सिंक्रनाइज़्ड वीडियो कैप्सूल बनाता है — प्रति स्लाइड एक TTS वॉइस-ओवर, वीडियो कैप्चर, सबटाइटल,
फ़ॉर्मैट रूपांतरण, ऑडियो पोस्ट-प्रोडक्शन, गुणवत्ता नियंत्रण और एक वैकल्पिक वर्टिकल
शॉर्ट-फ़ॉर्म (वायरल) पाइपलाइन।

```
AsciiDoc स्रोत → slider-gradle → reveal.js डेक + वक्ता स्क्रिप्ट
                                        ↓
        capsule-gradle → TTS (Piper/espeak) → ऑडियो इंजेक्शन → कैप्चर → WebM/MP4
                                        ↓                        ↓
                              Manim एनिमेशन (वैकल्पिक)   FFmpeg से 9:16 रीक्रॉप (वैकल्पिक)
```

CCCP Education बहु-प्लगइन पारिस्थितिकी तंत्र का हिस्सा (`groupId: education.cccp`)।

## आवश्यकताएँ

- **Java** 25+ (Kotlin 2.4.10 टूलचेन)
- **Gradle** 9.8+
- **Piper** — स्थानीय ऑफ़लाइन TTS (डिफ़ॉल्ट इंजन)
- **espeak / espeak-ng** — TTS फ़ॉलबैक (`espeak` अनुपस्थित होने पर `espeak-ng` स्वतः उपयोग होता है)
- **Chromium** — पहले रन पर Playwright द्वारा स्वतः डाउनलोड
- **FFmpeg** — 9:16 रीक्रॉप, सबटाइटल burn-in, MP4 रूपांतरण, ऑडियो पोस्ट
- **Manim** — वैकल्पिक, एनिमेटेड गणितीय स्लाइड हेतु
- **Node** — वैकल्पिक, Remotion कैप्चर/एनिमेशन रणनीति हेतु

## त्वरित आरंभ

### 1. प्लगइन लागू करें

```gradle
plugins {
    id("education.cccp.capsule") version "0.0.8"
}
```

### 2. कॉन्फ़िगर करें

```gradle
capsule {
    outputDir.set("capsules")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    ttsEngine.set("espeak")   // "piper" (डिफ़ॉल्ट) या "espeak"
}
```

### 3. वीडियो बनाएँ

```bash
./gradlew generateCapsuleVideo
```

यह पूरी पाइपलाइन चलाता है: स्क्रिप्ट सत्यापन → TTS संश्लेषण → डेक HTML में ऑडियो इंजेक्शन →
हेडलेस कैप्चर → वीडियो आउटपुट।

### slider के साथ पूरी पाइपलाइन

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
./gradlew generateCapsuleVideo    # slider डेक + स्क्रिप्ट → वीडियो कैप्सूल
```

## उपलब्ध टास्क

| टास्क | समूह | विवरण |
|-------|------|-------|
| `generateCapsuleScript` | generate | slider द्वारा बनाई `*-script.txt` पढ़ता और कैप्सूल स्क्रिप्ट सत्यापित करता है |
| `generateCapsule` | generate | TTS ऑडियो फ़ाइलें (Piper/espeak) संश्लेषित करता है, प्रति स्लाइड एक |
| `generateCapsuleVideo` | generate | TTS ऑडियो को डेक HTML में इंजेक्ट करता है, फिर वीडियो कैप्चर करता है |
| `generateCapsuleVideoAllLanguages` | generate | प्रति लक्ष्य भाषा एक स्थानीयकृत कैप्सूल (`LanguageCatalog`) |
| `extractSpeakerNotes` | capsule | AsciiDoc वक्ता-नोट्स को सादे-पाठ `*-script.txt` अनुबंध में पार्स करता है |
| `translateAndExtractSpeakerNotes` | capsule | संयुक्त: `translateDeck` (slider) → `extractSpeakerNotes` |
| `translateAndGenerateCapsuleVideos` | capsule | संयुक्त: `translateDeck` → `extractSpeakerNotes` → `generateCapsuleVideoAllLanguages` |
| `generateCapsuleContent` | generate | LLM पाइपलाइन (koog): propose-context → सत्यापन → वक्ता-नोट्स + TTS स्क्रिप्ट |
| `generateCapsuleContentAndVideos` | capsule | संयुक्त: `generateCapsuleContent` → `translateAndGenerateCapsuleVideos` |
| `collectCapsuleAugmentedContext` | collect | संवर्धित संदर्भ एकत्र करता है (गवर्नेंस + RAG + Docs + परिदृश्य + शब्दावली) |
| `capsuleAiSmokeTest` | generate | न्यूनतम प्रॉम्प्ट से codebase LLM ब्रिज का स्मोक टेस्ट |
| `generateCapsuleTranscript` | generate | वक्ता-नोट्स से AsciiDoc ट्रांसक्रिप्ट लेख बनाता है |
| `generateCapsuleChapters` | generate | अध्याय मार्कर + YouTube टेक्स्ट अध्याय + विवरण |
| `generateCapsulePodcast` | generate | केवल-ऑडियो पॉडकास्ट (जोड़ी गई वाचन MP3) |
| `deployCapsule` | deploy | FFmpeg से कैप्सूल को वर्टिकल 9:16 (TikTok/Shorts) में रीक्रॉप करता है |
| `distributeCapsuleVideo` | distribute | अंतिम वीडियो को संस्करणित गंतव्य निर्देशिका में कॉपी करता है |
| `collectCapsuleContext` / `transformCapsuleContext` / `collectCapsuleRetrieve` | collect/transform | कैप्सूल संदर्भ JSON निर्यात / पार्स करता है (N3 इंजन अनुबंध) |
| `scaffoldCapsuleContext` | generate | टिप्पणीयुक्त डिफ़ॉल्ट `capsule-context.yml` बनाता है |
| `validateCapsuleVideoDuration` | verification | वीडियो अवधि की तुलना TTS ऑडियो अवधियों के योग से करता है (opt-in) |
| `preflightCapsuleContent` | verification | सशुल्क TTS से पहले सामग्री पूर्व-उड़ान (opt-in) |
| `renderQcBoards` | verification | QC बोर्ड (beats + «कहा vs दिखाया») रेंडर करता है (opt-in) |
| `verifyCapsuleVoice` | verification | ASR द्वारा वॉइस QC — निष्ठा, श्वास, टेक चयन (opt-in) |
| `selectBestVoiceTake` | verification | किसी स्लाइड के उम्मीदवारों में से सर्वोत्तम टेक चुनता है (opt-in) |
| `generateViralCampaign` | generate | Storyboard → शॉर्ट अभियान (YouTube/TikTok) (opt-in) |
| `renderViralVariants` / `renderViralThumbnails` / `renderViralSubtitles` | generate | वायरल वेरिएंट, थंबनेल, कराओके सबटाइटल रेंडर करता है (opt-in) |
| `validateViralCampaign` | verification | उत्पन्न फ़ाइलों के विरुद्ध मैनिफ़ेस्ट जाँचता है (opt-in) |
| `normalizeViralStoryboard` | generate | storyboard के अनुपस्थित ट्रांज़िशन भरता है (लेखन सहायक) |

> वैकल्पिक (*opt-in*) क्षमताएँ **डिफ़ॉल्ट रूप से अक्षम** (`false`) हैं और मुख्य पाइपलाइन को
> कभी अवरुद्ध नहीं करतीं।

## कॉन्फ़िगरेशन

प्रत्येक मान **4 स्रोतों** से हल होता है (प्राथमिकता **CLI > YAML > gradle.properties > ENV**):

```bash
./gradlew scaffoldCapsuleContext          # टिप्पणीयुक्त capsule-context.yml बनाता है
./gradlew generateCapsuleVideo -Pcapsule.tts.engine=espeak -Pcapsule.manim.quality=h
```

प्रतिनिधि DSL (पूर्ण 18-खंड `CapsuleConfig` हेतु [आंतरिक मार्गदर्शिका](../capsule-plugin/README.md) देखें):

```gradle
capsule {
    // इनपुट
    outputDir.set("capsule")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")

    // TTS
    ttsEngine.set("piper")                 // "piper" (डिफ़ॉल्ट) या "espeak"
    ttsVoice.set("fr_FR-siwis-medium")
    piperExecutablePath.set("piper")
    ttsFallbackEnabled.set(true)
    espeakVoice.set("fr")
    espeakSpeed.set(150)
    ttsLanguage.set("fr")                  // 22 भाषाएँ (LanguageCatalog)

    // कैप्चर / आउटपुट
    viewportWidth.set(1408)
    viewportHeight.set(792)
    captureStrategy.set("playwright")      // "playwright" | "screenshot" | "remotion"
    outputFormat.set("webm")               // "webm" (डिफ़ॉल्ट) | "mp4" | "both"
    outputBt709.set(false)                 // MP4 को BT.709 के रूप में टैग करें (opt-in)

    // सबटाइटल
    subtitleEnabled.set(false)
    subtitleFormat.set("srt")              // "srt" या "vtt"
    subtitleBurnIn.set(false)

    // वितरण
    ffmpegExecutablePath.set("ffmpeg")
    distribOutputWidth.set(1080)
    distribOutputHeight.set(1920)

    // Manim (वैकल्पिक)
    manimExecutablePath.set("manim")
    manimQuality.set("l")                  // l, m, h, p, k
}
```

## बहु-भाषा पाइपलाइन

एक स्रोत डेक → प्रति लक्ष्य भाषा एक स्थानीयकृत कैप्सूल। पाइपलाइन `slider.translateDeck` द्वारा
उत्पन्न अनूदित डेक उपभोग करती है (**22 भाषाएँ**, N0 `i18n-contracts` की `LanguageCatalog`),
और प्रति-भाषा TTS वॉइस को शुद्ध डोमेन `capsule.multilang` (`MultiLanguageResolver` +
`VoiceMapping`) से हल करती है।

```
स्रोत डेक (AsciiDoc)
        ↓ slider.translateDeck (LanguageCatalog, 22 भाषाएँ)
<deckName>_<lang>-deck.html (प्रति भाषा एक)
        ↓ capsule.extractSpeakerNotes
<deckName>_<lang>-script.txt (प्रति भाषा एक)
        ↓ capsule.generateCapsuleVideoAllLanguages
<deckName>_<lang>.webm (प्रति भाषा एक स्थानीयकृत कैप्सूल)
```

नामकरण परंपरा (सीमा अनुबंध, slider के साथ संरेखित):

- अनूदित डेक: `<deckName>_<lang>-deck.html`
- वक्ता स्क्रिप्ट: `<deckName>_<lang>-script.txt`
- स्थानीयकृत वीडियो: `<deckName>_<lang>.webm`

```bash
./gradlew generateCapsuleVideoAllLanguages   # डेक+स्क्रिप्ट जोड़ी वाली प्रत्येक भाषा हेतु एक वीडियो
./gradlew translateAndGenerateCapsuleVideos  # पूर्ण संयुक्त: translate → extract → वीडियो
```

बिना मेल खाती डेक + स्क्रिप्ट जोड़ी वाली भाषा चुपचाप छोड़ दी जाती है, और
**स्याही-अर्थव्यवस्था** नियम लागू होता है: जिस भाषा का वीडियो पहले से मौजूद और वैध हो, वह
कभी दोबारा रेंडर नहीं होता।

## वैकल्पिक सुविधाएँ

- **आउटपुट फ़ॉर्मैट** — `outputFormat = "mp4"` (या `"both"`) मूल WebM के अतिरिक्त MP4/H.264
  बनाता है; `outputBt709 = true` उसे BT.709 के रूप में टैग करता है (ब्रॉडकास्ट रंगमिति)।
- **ऑडियो पोस्ट-प्रोडक्शन** — पृष्ठभूमि संगीत, EBU R128 लाउडनेस (2-पास मापन) और sidechain
  ducking (`audioPost { … }`)।
- **सबटाइटल** — SRT/VTT निर्माण और burn-in (`subtitleEnabled`, `subtitleBurnIn`)।
- **गुणवत्ता नियंत्रण (opt-in)** — सामग्री पूर्व-उड़ान (`preflightCapsuleContent`), डिकोडिंग
  द्वारा वीडियो अखंडता, नियतात्मक timeline, QC बोर्ड (`renderQcBoards`) और ASR द्वारा वॉइस QC
  (`verifyCapsuleVoice`, `selectBestVoiceTake`)।
- **Transcript** — उल्टी पाइपलाइन: वक्ता-नोट्स से AsciiDoc लेख (`TEMPLATE` या `LLM`)।
- **Podcast** — केवल-ऑडियो वाचन MP3।
- **Preview** — dry-run (`previewOnly = true`): केवल स्लाइड PNG रेंडर करता है, mux छोड़ देता है।
- **वायरल (opt-in)** — सत्यापन-योग्य storyboard → संवर्धित संदर्भ पर आधारित hooks/CTA के साथ
  वर्टिकल 9:16 शॉर्ट अभियान, थंबनेल, कराओके सबटाइटल और `CampaignBundle`।

### स्लाइड लय (Remotion कैप्चर)

`captureStrategy = "remotion"` के साथ, डेक फ़िल्माए जाने के बजाय एनिमेट होता है: स्लाइड का
प्रत्येक ब्लॉक पूरी स्लाइड के एक साथ आने के बजाय पढ़ने की लय में आता है। डिफ़ॉल्ट beats
(स्लाइड के आरंभ से सेकंड): `.kicker` 0.0 · `h1`/`h2` 0.15 · `.rule` 0.5 · `.lede` 1.0 ·
`.stat` 4.5 · `.statlabel` 5.0। `.brand`, `.num` और `.grid` chrome हैं (पहले फ़्रेम से मौजूद)।
कोई भी ब्लॉक `data-at` से अपना beat सेट कर सकता है:

```html
<p class="stat" data-at="8.5">542 076 km</p>
```

## बिल्ड व टेस्ट (योगदानकर्ता)

```bash
./gradlew build                    # पूर्ण बिल्ड
./gradlew check                    # यूनिट + कार्यात्मक + Cucumber
./gradlew test                     # केवल JUnit5 यूनिट
./gradlew cucumberTest -PrunCucumber
./gradlew functionalTest           # Gradle TestKit
./gradlew publishToMavenLocal      # स्थानीय प्रकाशन
```

## समस्या-निवारण

| लक्षण | समाधान |
|--------|--------|
| Playwright Chromium नहीं मिला | एक बार चलाकर स्वतः डाउनलोड करें, या `chromiumExecutablePath` सेट करें |
| Piper TTS से ऑडियो नहीं | `piperExecutablePath` जाँचें, सुनिश्चित करें कि वॉइस मॉडल स्थापित है |
| `deployCapsule` विफल | सत्यापित करें कि FFmpeg `PATH` में है (`ffmpeg -version`) |
| Manim स्लाइड रेंडर नहीं | Manim के बिना परीक्षण हेतु `manimExecutablePath` को `noop` सेट करें |
| वीडियो कैप्चर टाइमआउट | `playwrightTimeout` बढ़ाएँ (डिफ़ॉल्ट 120000 ms) |
| अपर्याप्त मेमोरी | `export GRADLE_OPTS="-Xmx2g"` |

## दस्तावेज़

- [capsule-plugin/README.md](../capsule-plugin/README.md) — प्लगइन आंतरिक (डेवलपर व योगदानकर्ता मार्गदर्शिका)
- [capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) — पाइपलाइन आर्किटेक्चर (PlantUML)

## लाइसेंस

Apache License 2.0 — देखें [LICENSE](../LICENSE).

---

_CCCP Education पारिस्थितिकी तंत्र का भाग — `groupId: education.cccp`।_
