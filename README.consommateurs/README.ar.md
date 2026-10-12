<!-- translated from README.md rev 0.0.8 -->
# capsule-gradle — دليل المستخدم

> إضافة Gradle تحوّل عرض reveal.js إلى كبسولة فيديو متزامنة — تعليق TTS، التقاط، ترجمات، تحويل صيغة، ما بعد الصوت وخط قصير/فيروسي — دون أي ما بعد إنتاج بشري.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.8&color=green)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![الرخصة](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=%D8%A7%D9%84%D8%B1%D8%AE%D8%B5%D8%A9)](../LICENSE)

- **الإصدار**: `0.0.8` (Maven Central) · **المجموعة**: `education.cccp` · **معرّف الإضافة**: `education.cccp.capsule`
- **يتطلّب**: Java 25+ · Gradle 9.8+
- **الوثائق**: [داخليّات الإضافة](../capsule-plugin/README.md) · [معمارية خط الأنابيب](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc)

🌐 اللغات: [English](../README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | **العربية** | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## ماذا تفعل

تستهلك `capsule-gradle` عرض reveal.js (منتَج بواسطة `slider-gradle`) وتنتج كبسولة فيديو
متزامنة — تعليق TTS لكل شريحة، والتقاط الفيديو، والترجمات، وتحويل الصيغة، وما بعد إنتاج
الصوت، وضبط الجودة، وخط عمودي قصير/فيروسي اختياري.

```
مصدر AsciiDoc → slider-gradle → عرض reveal.js + سكربت المتحدّث
                                        ↓
        capsule-gradle → TTS (Piper/espeak) → حقن الصوت → الالتقاط → WebM/MP4
                                        ↓                        ↓
                              رسوم Manim (اختياري)   إعادة قصّ 9:16 عبر FFmpeg (اختياري)
```

جزء من منظومة CCCP Education متعدّدة الإضافات (`groupId: education.cccp`).

## المتطلّبات

- **Java** 25+ (سلسلة أدوات Kotlin 2.4.10)
- **Gradle** 9.8+
- **Piper** — TTS محلي دون اتصال (المحرّك الافتراضي)
- **espeak / espeak-ng** — TTS احتياطي (يُستخدم `espeak-ng` تلقائيًا عند غياب `espeak`)
- **Chromium** — يُنزّله Playwright تلقائيًا عند أول تشغيل
- **FFmpeg** — إعادة قصّ 9:16، burn-in للترجمات، تحويل MP4، ما بعد الصوت
- **Manim** — اختياري، للشرائح الرياضية المتحرّكة
- **Node** — اختياري، لاستراتيجية الالتقاط/التحريك Remotion

## البدء السريع

### ١. تطبيق الإضافة

```gradle
plugins {
    id("education.cccp.capsule") version "0.0.8"
}
```

### ٢. التهيئة

```gradle
capsule {
    outputDir.set("capsules")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    ttsEngine.set("espeak")   // "piper" (الافتراضي) أو "espeak"
}
```

### ٣. توليد الفيديو

```bash
./gradlew generateCapsuleVideo
```

يشغّل هذا خط الأنابيب كاملًا: التحقّق من السكربت → تركيب TTS → حقن الصوت في HTML العرض →
الالتقاط بلا واجهة → إخراج الفيديو.

### خط الأنابيب الكامل مع slider

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
./gradlew generateCapsuleVideo    # عرض + سكربت slider → كبسولة فيديو
```

## المهام المتاحة

| المهمة | المجموعة | الوصف |
|--------|----------|-------|
| `generateCapsuleScript` | generate | يقرأ `*-script.txt` المنتَج بواسطة slider ويتحقّق من سكربت الكبسولة |
| `generateCapsule` | generate | يركّب ملفات صوت TTS (Piper/espeak)، واحدًا لكل شريحة |
| `generateCapsuleVideo` | generate | يحقن صوت TTS في HTML العرض ثم يلتقط الفيديو |
| `generateCapsuleVideoAllLanguages` | generate | كبسولة مترجمة لكل لغة هدف (`LanguageCatalog`) |
| `extractSpeakerNotes` | capsule | يحلّل ملاحظات المتحدّث AsciiDoc إلى عقد `*-script.txt` نصّي |
| `translateAndExtractSpeakerNotes` | capsule | مركّبة: `translateDeck` (slider) → `extractSpeakerNotes` |
| `translateAndGenerateCapsuleVideos` | capsule | مركّبة: `translateDeck` → `extractSpeakerNotes` → `generateCapsuleVideoAllLanguages` |
| `generateCapsuleContent` | generate | خط LLM (koog): propose-context → تحقّق → ملاحظات المتحدّث + سكربت TTS |
| `generateCapsuleContentAndVideos` | capsule | مركّبة: `generateCapsuleContent` → `translateAndGenerateCapsuleVideos` |
| `collectCapsuleAugmentedContext` | collect | يجمع السياق المعزّز (حوكمة + RAG + Docs + سيناريو + مسرد) |
| `capsuleAiSmokeTest` | generate | اختبار دخان لجسر LLM في codebase بمطالبة بسيطة |
| `generateCapsuleTranscript` | generate | ينتج مقال نصّ AsciiDoc من ملاحظات المتحدّث |
| `generateCapsuleChapters` | generate | علامات الفصول + فصول YouTube النصّية + الوصف |
| `generateCapsulePodcast` | generate | بودكاست صوتي فقط (سرد MP3 مدمج) |
| `deployCapsule` | deploy | يعيد قصّ الكبسولة إلى عمودي 9:16 (TikTok/Shorts) عبر FFmpeg |
| `distributeCapsuleVideo` | distribute | ينسخ الفيديوهات النهائية إلى مجلّد وجهة مُصدَّر |
| `collectCapsuleContext` / `transformCapsuleContext` / `collectCapsuleRetrieve` | collect/transform | يصدّر / يحلّل JSON سياق الكبسولة (عقد محرّك N3) |
| `scaffoldCapsuleContext` | generate | يولّد `capsule-context.yml` افتراضيًا مع تعليقات |
| `validateCapsuleVideoDuration` | verification | يقارن مدّة الفيديو بمجموع مدد صوت TTS (opt-in) |
| `preflightCapsuleContent` | verification | فحص المحتوى المسبق قبل TTS المدفوع (opt-in) |
| `renderQcBoards` | verification | يرسم لوحات QC (beats + «قيل مقابل عُرض») (opt-in) |
| `verifyCapsuleVoice` | verification | QC الصوت عبر ASR — الدقّة، النَّفَس، اختيار المأخذ (opt-in) |
| `selectBestVoiceTake` | verification | يختار أفضل مأخذ بين مرشّحي الشريحة (opt-in) |
| `generateViralCampaign` | generate | Storyboard → حملة قصيرة (YouTube/TikTok) (opt-in) |
| `renderViralVariants` / `renderViralThumbnails` / `renderViralSubtitles` | generate | يرسم المتغيّرات الفيروسية، الصور المصغّرة، ترجمات الكاريوكي (opt-in) |
| `validateViralCampaign` | verification | يتحقّق من البيان مقابل الملفات المنتَجة (opt-in) |
| `normalizeViralStoryboard` | generate | يكمل انتقالات storyboard المفقودة (مساعد تأليف) |

> القدّرات الاختيارية (*opt-in*) **معطّلة افتراضيًا** (`false`) ولا تعطّل خط الأنابيب
> الرئيسي أبدًا.

## التهيئة

تُحلّ كل قيمة من **٤ مصادر** (الأولوية **CLI > YAML > gradle.properties > ENV**):

```bash
./gradlew scaffoldCapsuleContext          # يولّد capsule-context.yml مع تعليقات
./gradlew generateCapsuleVideo -Pcapsule.tts.engine=espeak -Pcapsule.manim.quality=h
```

DSL تمثيلي (راجع [دليل الداخليّات](../capsule-plugin/README.md) للحصول على `CapsuleConfig`
الكامل ذي 18 قسمًا):

```gradle
capsule {
    // الإدخال
    outputDir.set("capsule")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")

    // TTS
    ttsEngine.set("piper")                 // "piper" (الافتراضي) أو "espeak"
    ttsVoice.set("fr_FR-siwis-medium")
    piperExecutablePath.set("piper")
    ttsFallbackEnabled.set(true)
    espeakVoice.set("fr")
    espeakSpeed.set(150)
    ttsLanguage.set("fr")                  // 22 لغة (LanguageCatalog)

    // الالتقاط / الإخراج
    viewportWidth.set(1408)
    viewportHeight.set(792)
    captureStrategy.set("playwright")      // "playwright" | "screenshot" | "remotion"
    outputFormat.set("webm")               // "webm" (الافتراضي) | "mp4" | "both"
    outputBt709.set(false)                 // وسم MP4 بـ BT.709 (opt-in)

    // الترجمات
    subtitleEnabled.set(false)
    subtitleFormat.set("srt")              // "srt" أو "vtt"
    subtitleBurnIn.set(false)

    // التوزيع
    ffmpegExecutablePath.set("ffmpeg")
    distribOutputWidth.set(1080)
    distribOutputHeight.set(1920)

    // Manim (اختياري)
    manimExecutablePath.set("manim")
    manimQuality.set("l")                  // l, m, h, p, k
}
```

## خط الأنابيب متعدّد اللغات

عرض مصدر واحد → كبسولة مترجمة لكل لغة هدف. يستهلك خط الأنابيب العروض المترجمة التي ينتجها
`slider.translateDeck` (**22 لغة**، `LanguageCatalog` من عقد N0 `i18n-contracts`)، ويحلّ صوت
TTS لكل لغة عبر النطاق النقي `capsule.multilang` (`MultiLanguageResolver` + `VoiceMapping`).

```
عرض المصدر (AsciiDoc)
        ↓ slider.translateDeck (LanguageCatalog، 22 لغة)
<deckName>_<lang>-deck.html (واحد لكل لغة)
        ↓ capsule.extractSpeakerNotes
<deckName>_<lang>-script.txt (واحد لكل لغة)
        ↓ capsule.generateCapsuleVideoAllLanguages
<deckName>_<lang>.webm (كبسولة مترجمة واحدة لكل لغة)
```

اصطلاح التسمية (عقد حدّي، متوافق مع slider):

- العرض المترجم: `<deckName>_<lang>-deck.html`
- سكربت المتحدّث: `<deckName>_<lang>-script.txt`
- الفيديو المترجم: `<deckName>_<lang>.webm`

```bash
./gradlew generateCapsuleVideoAllLanguages   # فيديو واحد لكل لغة تملك زوج عرض+سكربت
./gradlew translateAndGenerateCapsuleVideos  # مركّبة كاملة: translate → extract → فيديوهات
```

تُتجاهل بصمت أي لغة بلا زوج عرض + سكربت مطابق، وتُطبَّق قاعدة **اقتصاد الحبر**: اللغة التي
فيديوها موجود مسبقًا وسليم لا يُعاد رسمه أبدًا.

## الميزات الاختيارية

- **صيغة الإخراج** — `outputFormat = "mp4"` (أو `"both"`) ينتج MP4/H.264 إضافةً إلى WebM
  الأصلي؛ `outputBt709 = true` يوسمه كـ BT.709 (تلوين بثّي).
- **ما بعد إنتاج الصوت** — موسيقى خلفية، جهارة EBU R128 (قياس بمرّتين) و ducking جانبي
  (`audioPost { … }`).
- **الترجمات** — توليد SRT/VTT و burn-in (`subtitleEnabled`، `subtitleBurnIn`).
- **ضبط الجودة (opt-in)** — فحص المحتوى المسبق (`preflightCapsuleContent`)، سلامة الفيديو
  عبر فكّ الترميز، timeline حتمي، لوحات QC (`renderQcBoards`) و QC الصوت عبر ASR
  (`verifyCapsuleVoice`، `selectBestVoiceTake`).
- **Transcript** — خط معاكس: مقال AsciiDoc من ملاحظات المتحدّث (`TEMPLATE` أو `LLM`).
- **Podcast** — MP3 صوتي فقط للسرد.
- **Preview** — dry-run (`previewOnly = true`): يرسم فقط صور شرائح PNG ويتجاوز الدمج.
- **الفيروسي (opt-in)** — storyboard قابل للتحقّق → حملة قصيرة عمودية 9:16 بـ hooks/CTA
  مرتكزة على السياق المعزّز، وصور مصغّرة، وترجمات كاريوكي، و`CampaignBundle`.

### إيقاع الشرائح (التقاط Remotion)

مع `captureStrategy = "remotion"`، يُحرَّك العرض بدلًا من تصويره: يظهر كل كتلة من الشريحة
بإيقاع قراءة بدلًا من ظهور الشريحة كاملة دفعة واحدة. الإيقاعات الافتراضية (ثوانٍ من بداية
الشريحة): `.kicker` 0.0 · `h1`/`h2` 0.15 · `.rule` 0.5 · `.lede` 1.0 · `.stat` 4.5 ·
`.statlabel` 5.0. `.brand` و `.num` و `.grid` من chrome (حاضرة من الإطار الأول). يمكن لأي
كتلة أن تحدّد إيقاعها بـ `data-at`:

```html
<p class="stat" data-at="8.5">542 076 km</p>
```

## البناء والاختبار (المساهمون)

```bash
./gradlew build                    # بناء كامل
./gradlew check                    # وحدة + وظيفية + Cucumber
./gradlew test                     # وحدة JUnit5 فقط
./gradlew cucumberTest -PrunCucumber
./gradlew functionalTest           # Gradle TestKit
./gradlew publishToMavenLocal      # نشر محلي
```

## حلّ المشاكل

| العَرَض | الحلّ |
|---------|-------|
| Chromium من Playwright غير موجود | شغّل مرّة للتنزيل التلقائي، أو اضبط `chromiumExecutablePath` |
| Piper TTS لا ينتج صوتًا | تحقّق من `piperExecutablePath`، وتأكّد من تثبيت نموذج الصوت |
| `deployCapsule` يفشل | تأكّد من وجود FFmpeg في `PATH` (`ffmpeg -version`) |
| شرائح Manim غير مرسومة | اضبط `manimExecutablePath` على `noop` للاختبار بلا Manim |
| انتهاء مهلة التقاط الفيديو | ارفع `playwrightTimeout` (الافتراضي 120000 ms) |
| ذاكرة غير كافية | `export GRADLE_OPTS="-Xmx2g"` |

## الوثائق

- [capsule-plugin/README.md](../capsule-plugin/README.md) — داخليّات الإضافة (دليل المطوّر والمساهم)
- [capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) — معمارية خط الأنابيب (PlantUML)

## الرخصة

Apache License 2.0 — انظر [LICENSE](../LICENSE).

---

_جزء من منظومة CCCP Education — `groupId: education.cccp`._
