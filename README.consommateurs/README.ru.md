<!-- translated from README.md rev 0.0.1-SNAPSHOT -->
# capsule-gradle — Руководство потребителя

> Плагин Gradle для автоматизированной генерации педагогических видеокапсул из презентаций reveal.js.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.1-SNAPSHOT&color=orange)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=License)](../LICENSE)

- **Версия**: `0.0.1-SNAPSHOT` (не опубликовано) · **Группа**: `education.cccp` · **ID плагина**: `education.cccp.capsule`
- **Сборка**: `./gradlew build` · **Тесты**: `./gradlew check` (195 unit + 22 Cucumber PASS)
- **Покрытие**: отчёты Kover XML + HTML встроены в `check`

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | **Русский** | [اردو](README.ur.md)

---

## Что делает

`capsule-gradle` преобразует презентацию reveal.js (созданную `slider-gradle`) в
педагогическую видеокапсулу с синхронным TTS-озвучиванием — без постпродакшена.

Пайплайн оркеструет четыре внешних инструмента:

```
AsciiDoc source → slider-gradle → reveal.js deck + capsule script
                                      ↓
              capsule-gradle → TTS (Piper/espeak) → audio injection → Playwright capture → WebM video
                                      ↓                              ↓
                              Manim animation (optional)      FFmpeg distrib (9:16)
```

Часть мультиплагинной экосистемы CCCP Education (`groupId: education.cccp`).

## Быстрый старт

### 1. Применить плагин

```gradle
plugins {
    id("education.cccp.capsule") version "0.0.1-SNAPSHOT"
}
```

### 2. Настроить капсулу

```gradle
capsule {
    outputDir.set("capsules")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    ttsEngine.set("espeak")
}
```

### 3. Сгенерировать видео

```bash
./gradlew generateCapsuleVideo
```

Запускает весь пайплайн: валидация скрипта → синтез TTS → инъекция аудио
в HTML презентации → захват Playwright headless → вывод WebM.

### Полный пайплайн со slider

Корневой `build.gradle.kts` связывает slider и capsule вместе:

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
./gradlew capsulevideo    # презентация slider + скрипт → видеокапсула
```

## Доступные задачи

| Задача | Группа | Описание |
|------|-------|-------------|
| `generateCapsuleScript`   | generate  | Читает `*-script.txt` из slider и валидирует скрипт капсулы |
| `generateCapsule`          | generate  | Генерирует аудиофайлы TTS из скриптов капсулы (Piper/espeak) |
| `generateCapsuleVideo`     | generate  | Внедряет TTS-аудио в HTML презентации, затем захватывает видео через Playwright |
| `deployCapsule`            | deploy    | Обрезает капсулы в вертикальный формат 9:16 (TikTok/Shorts) через FFmpeg |
| `collectCapsuleContext`    | collect   | Экспортирует контекст капсул (пути видео + метаданные) как JSON для движка N3 |
| `transformCapsuleContext`  | transform | Разбирает `capsule-context.json` и возвращает список презентаций |
| `collectCapsuleRetrieve`   | collect   | Извлекает презентации капсул из JSON-контекста (контракт движка N3) |
| `scaffoldCapsuleContext`   | generate  | Создаёт файл конфигурации `capsule-context.yml` по умолчанию с комментариями |

## DSL расширения

```gradle
capsule {
    // Ввод
    outputDir.set("capsule")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    chromiumExecutablePath.set("")

    // TTS
    ttsEngine.set("piper")                    // "piper" или "espeak"
    ttsVoice.set("fr_FR-siwis-medium")
    piperExecutablePath.set("piper")
    ttsFallbackEnabled.set(true)
    espeakVoice.set("fr")
    espeakSpeed.set(150)
    ttsLanguage.set("fr")                     // fr, en, es, de

    // Захват
    viewportWidth.set(1408)
    viewportHeight.set(792)
    playwrightTimeout.set(120_000.0)
    slideDurationSeconds.set(5.0)
    parallelCaptureEnabled.set(false)
    parallelCaptureThreads.set(4)

    // Субтитры
    subtitleEnabled.set(false)
    subtitleFormat.set("srt")                 // "srt" или "vtt"
    subtitleBurnIn.set(false)
    subtitleBurnInFontSize.set(24)
    subtitleBurnInFontColor.set("&H00FFFFFF")
    subtitleBurnInOutlineColor.set("&H00000000")
    subtitleBurnInPosition.set("bottom")      // "bottom" или "top"

    // Дистрибуция
    ffmpegExecutablePath.set("ffmpeg")
    distribOutputWidth.set(1080)
    distribOutputHeight.set(1920)

    // Анимация Manim
    manimExecutablePath.set("manim")
    manimQuality.set("l")                     // l, m, h, p, k
    manimScriptsDir.set("src/manim")
    manimOutputDir.set("build/capsule/manim")
    manimParallelRender.set(false)
    manimParallelRenderThreads.set(4)
}
```

### Многоисточниковая конфигурация

Конфигурация разрешается из 4 источников (приоритет: CLI > YAML > gradle.properties > ENV):

```bash
./gradlew scaffoldCapsuleContext          # сгенерировать capsule-context.yml
./gradlew generateCapsuleVideo -Pcapsule.tts.engine=espeak -Pcapsule.manim.quality=h
```

## Предварительные требования

- **Java** 24+ (toolchain Kotlin 2.3.20)
- **Gradle** 9.5+
- **Piper** (TTS, локальный офлайн) — движок по умолчанию
- **espeak** (резервный TTS)
- **Chromium** (автозагружается Playwright при первом запуске)
- **FFmpeg** (для `deployCapsule` обрезки 9:16 и вшития субтитров)
- **Manim** (опционально, для слайдов математической анимации)

## Сборка и тесты

```bash
./gradlew build                    # полная сборка
./gradlew check                    # unit + функциональные + Cucumber тесты
./gradlew test                     # JUnit5 unit-тесты (исключает @integration)
./gradlew cucumberTest             # BDD-тесты Cucumber
./gradlew functionalTest           # функциональные тесты Gradle TestKit
./gradlew publishToMavenLocal      # локальная публикация
```

## Устранение неполадок

| Симптом | Решение |
|---------|-----|
| Playwright Chromium не найден     | запустите один раз для автозагрузки или задайте `chromiumExecutablePath` |
| Piper TTS не выдаёт аудио       | проверьте `piperExecutablePath`, убедитесь что модель голоса установлена |
| `deployCapsule` падает              | проверьте что FFmpeg в PATH (`ffmpeg -version`) |
| Слайды Manim не отрисованы          | задайте `manimExecutablePath` равным `noop` для тестирования без Manim |
| Таймаут захвата видео              | увеличьте `playwrightTimeout` (по умолчанию 120000ms) |
| Нехватка памяти                      | `export GRADLE_OPTS="-Xmx2g"` |

Подробнее о пайплайне см. [CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc).

## Лицензия

Apache License 2.0 — см. [LICENSE](../LICENSE).

---

_Часть экосистемы CCCP Education — `groupId: education.cccp`._