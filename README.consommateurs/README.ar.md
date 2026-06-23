<!-- translated from README.md rev 0.0.1-SNAPSHOT -->
# capsule-gradle — دليل المستهلك

> إضافة Gradle لتوليد كبسولات فيديو تعليمية تلقائياً من عروض reveal.js.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.1-SNAPSHOT&color=orange)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=License)](../LICENSE)

- **الإصدار**: `0.0.1-SNAPSHOT` (غير منشور) · **المجموعة**: `education.cccp` · **معرّف الإضافة**: `education.cccp.capsule`
- **البناء**: `./gradlew build` · **الاختبارات**: `./gradlew check` (195 وحدة + 22 Cucumber ناجحة)
- **التغطية**: تقارير Kover XML + HTML مدمجة في `check`

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | **العربية** | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## ماذا تفعل

`capsule-gradle` يحوّل عرض reveal.js (مُنتَج بواسطة `slider-gradle`) إلى
كبسولة فيديو تعليمية مع رواية TTS متزامنة — بدون مرحلة ما بعد الإنتاج.

تُنسّق الأنبوبة أربع أدوات خارجية:

```
AsciiDoc source → slider-gradle → reveal.js deck + capsule script
                                      ↓
              capsule-gradle → TTS (Piper/espeak) → audio injection → Playwright capture → WebM video
                                      ↓                              ↓
                              Manim animation (optional)      FFmpeg distrib (9:16)
```

جزء من منظومة CCCP Education متعددة الإضافات (`groupId: education.cccp`).

## البداية السريعة

### 1. تطبيق الإضافة

```gradle
plugins {
    id("education.cccp.capsule") version "0.0.1-SNAPSHOT"
}
```

### 2. ضبط الكبسولة

```gradle
capsule {
    outputDir.set("capsules")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    ttsEngine.set("espeak")
}
```

### 3. توليد الفيديو

```bash
./gradlew generateCapsuleVideo
```

يشغّل الأنبوبة الكاملة: التحقق من السكربت → توليف TTS → حقن الصوت
في HTML العرض → التقاط Playwright بدون واجهة → إخراج WebM.

### الأنبوبة الكاملة مع slider

يصل `build.gradle.kts` الجذري slider و capsule معاً:

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
./gradlew capsulevideo    # عرض slider + سكربت → فيديو الكبسولة
```

## المهام المتاحة

| المهمة | المجموعة | الوصف |
|------|-------|-------------|
| `generateCapsuleScript`   | generate  | يقرأ `*-script.txt` من slider ويتحقق من سكربت الكبسولة |
| `generateCapsule`          | generate  | يولّد ملفات صوت TTS من سكربتات الكبسولة (Piper/espeak) |
| `generateCapsuleVideo`     | generate  | يحقن صوت TTS في HTML العرض ثم يلتقط الفيديو عبر Playwright |
| `deployCapsule`            | deploy    | يعيد قص الكبسولات إلى عمودي 9:16 (TikTok/Shorts) عبر FFmpeg |
| `collectCapsuleContext`    | collect   | يصدّر سياق الكبسولات (مسارات الفيديو + البيانات الوصفية) كـ JSON لمحرك N3 |
| `transformCapsuleContext`  | transform | يحلّل `capsule-context.json` ويعيد قائمة بالعروض |
| `collectCapsuleRetrieve`   | collect   | يسترجع عروض الكبسولة من JSON السياق (عقد محرك N3) |
| `scaffoldCapsuleContext`   | generate  | ينشئ ملف ضبط `capsule-context.yml` افتراضي مع تعليقات |

## DSL الامتداد

```gradle
capsule {
    // الإدخال
    outputDir.set("capsule")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    chromiumExecutablePath.set("")

    // TTS
    ttsEngine.set("piper")                    // "piper" أو "espeak"
    ttsVoice.set("fr_FR-siwis-medium")
    piperExecutablePath.set("piper")
    ttsFallbackEnabled.set(true)
    espeakVoice.set("fr")
    espeakSpeed.set(150)
    ttsLanguage.set("fr")                     // fr, en, es, de

    // الالتقاط
    viewportWidth.set(1408)
    viewportHeight.set(792)
    playwrightTimeout.set(120_000.0)
    slideDurationSeconds.set(5.0)
    parallelCaptureEnabled.set(false)
    parallelCaptureThreads.set(4)

    // الترجمة المصاحبة
    subtitleEnabled.set(false)
    subtitleFormat.set("srt")                 // "srt" أو "vtt"
    subtitleBurnIn.set(false)
    subtitleBurnInFontSize.set(24)
    subtitleBurnInFontColor.set("&H00FFFFFF")
    subtitleBurnInOutlineColor.set("&H00000000")
    subtitleBurnInPosition.set("bottom")      // "bottom" أو "top"

    // التوزيع
    ffmpegExecutablePath.set("ffmpeg")
    distribOutputWidth.set(1080)
    distribOutputHeight.set(1920)

    // رسوم Manim المتحركة
    manimExecutablePath.set("manim")
    manimQuality.set("l")                     // l, m, h, p, k
    manimScriptsDir.set("src/manim")
    manimOutputDir.set("build/capsule/manim")
    manimParallelRender.set(false)
    manimParallelRenderThreads.set(4)
}
```

### الضبط متعدد المصادر

يُحلّ الضبط من 4 مصادر (الأولوية: CLI > YAML > gradle.properties > ENV):

```bash
./gradlew scaffoldCapsuleContext          # توليد capsule-context.yml
./gradlew generateCapsuleVideo -Pcapsule.tts.engine=espeak -Pcapsule.manim.quality=h
```

## المتطلبات المسبقة

- **Java** 24+ (سلسلة أدوات Kotlin 2.3.20)
- **Gradle** 9.5+
- **Piper** (TTS، محلي دون اتصال) — المحرك الافتراضي
- **espeak** (محرك TTS الاحتياطي)
- **Chromium** (يُنزّل تلقائياً بواسطة Playwright عند أول تشغيل)
- **FFmpeg** (لإعادة قص `deployCapsule` 9:16 وحرق الترجمة)
- **Manim** (اختياري، لشرائح الرسوم الرياضية المتحركة)

## البناء والاختبار

```bash
./gradlew build                    # بناء كامل
./gradlew check                    # اختبارات الوحدة + الوظيفية + Cucumber
./gradlew test                     # اختبارات وحدة JUnit5 (يستثني @integration)
./gradlew cucumberTest             # اختبارات BDD Cucumber
./gradlew functionalTest           # اختبارات وظيفية Gradle TestKit
./gradlew publishToMavenLocal      # النشر محلياً
```

## استكشاف الأخطاء

| العَرَض | الحل |
|---------|-----|
| Chromium لـ Playwright غير موجود     | شغّل مرة واحدة للتنزيل التلقائي، أو اضبط `chromiumExecutablePath` |
| Piper TTS لا يُنتج صوتاً       | تحقّق من `piperExecutablePath`، تأكد من تثبيت نموذج الصوت |
| فشل `deployCapsule`              | تحقّق أن FFmpeg على PATH (`ffmpeg -version`) |
| شرائح Manim لم تُعرض          | اضبط `manimExecutablePath` إلى `noop` للاختبار دون Manim |
| مهلة التقاط الفيديو              | زِد `playwrightTimeout` (الافتراضي 120000ms) |
| نفاد الذاكرة                      | `export GRADLE_OPTS="-Xmx2g"` |

راجع [CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) لتفاصيل الأنبوبة.

## الترخيص

Apache License 2.0 — راجع [LICENSE](../LICENSE).

---

_جزء من منظومة CCCP Education — `groupId: education.cccp`._