<!-- translated from README.md rev 0.0.10 -->
# capsule-gradle — Руководство пользователя

> Плагин Gradle, который превращает колоду reveal.js в синхронизированную видеокапсулу — озвучка TTS, захват, субтитры, конвертация формата, пост-обработка звука и короткий/вирусный конвейер — без ручной постпродакшн-работы.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.10&color=green)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![Лицензия](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=%D0%9B%D0%B8%D1%86%D0%B5%D0%BD%D0%B7%D0%B8%D1%8F)](../LICENSE)

- **Версия**: `0.0.10` (Maven Central) · **Группа**: `education.cccp` · **ID плагина**: `education.cccp.capsule`
- **Требуется**: Java 25+ · Gradle 9.8+
- **Документация**: [внутреннее устройство плагина](../capsule-plugin/README.md) · [архитектура конвейера](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc)

🌐 Языки: [English](../README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | **Русский** | [اردو](README.ur.md)

---

## Что делает

`capsule-gradle` потребляет колоду reveal.js (созданную `slider-gradle`) и создаёт
синхронизированную видеокапсулу — озвучку TTS для каждого слайда, захват видео, субтитры,
конвертацию формата, пост-обработку звука, контроль качества и опциональный вертикальный
короткий/вирусный конвейер.

```
Источник AsciiDoc → slider-gradle → колода reveal.js + скрипт докладчика
                                        ↓
        capsule-gradle → TTS (Piper/espeak) → вставка звука → захват → WebM/MP4
                                        ↓                        ↓
                              анимация Manim (опц.)   перекадрирование 9:16 через FFmpeg (опц.)
```

Часть мультиплагинной экосистемы CCCP Education (`groupId: education.cccp`).

## Требования

- **Java** 25+ (toolchain Kotlin 2.4.10)
- **Gradle** 9.8+
- **Piper** — локальный офлайн TTS (движок по умолчанию)
- **espeak / espeak-ng** — резервный TTS (`espeak-ng` используется автоматически, если `espeak` отсутствует)
- **Fish Audio** — опциональный облачный TTS с **клонированным голосом** (движок `fish`); пул ключей хранится в игнорируемом git `fish.yaml`
- **Chromium** — загружается Playwright автоматически при первом запуске
- **FFmpeg** — перекадрирование 9:16, burn-in субтитров, конвертация в MP4, аудио-пост
- **Manim** — опционально, для анимированных математических слайдов
- **Node** — опционально, для стратегии захвата/анимации Remotion

## Быстрый старт

### 1. Применить плагин

```gradle
plugins {
    id("education.cccp.capsule") version "0.0.10"
}
```

### 2. Настроить

```gradle
capsule {
    outputDir.set("capsules")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    ttsEngine.set("espeak")   // "piper" (по умолчанию), "espeak" или "fish" (Fish Audio cloud, клонированный голос)
}
```

### 3. Сгенерировать видео

```bash
./gradlew generateCapsuleVideo
```

Это запускает весь конвейер: проверка скрипта → синтез TTS → вставка звука в HTML колоды →
захват без интерфейса → вывод видео.

### Полный конвейер со slider

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
./gradlew generateCapsuleVideo    # колода + скрипт slider → видеокапсула
```

## Доступные задачи

| Задача | Группа | Описание |
|--------|--------|----------|
| `generateCapsuleScript` | generate | Читает `*-script.txt`, созданный slider, и проверяет скрипт капсулы |
| `generateCapsule` | generate | Синтезирует аудиофайлы TTS (Piper/espeak), по одному на слайд |
| `generateCapsuleVideo` | generate | Вставляет аудио TTS в HTML колоды, затем захватывает видео |
| `generateCapsuleVideoAllLanguages` | generate | По одной локализованной капсуле на каждый целевой язык (`LanguageCatalog`) |
| `extractSpeakerNotes` | capsule | Разбирает заметки докладчика AsciiDoc в текстовый контракт `*-script.txt` |
| `translateAndExtractSpeakerNotes` | capsule | Составная: `translateDeck` (slider) → `extractSpeakerNotes` |
| `translateAndGenerateCapsuleVideos` | capsule | Составная: `translateDeck` → `extractSpeakerNotes` → `generateCapsuleVideoAllLanguages` |
| `generateCapsuleContent` | generate | Конвейер LLM (koog): propose-context → валидация → заметки докладчика + скрипт TTS |
| `generateCapsuleContentAndVideos` | capsule | Составная: `generateCapsuleContent` → `translateAndGenerateCapsuleVideos` |
| `collectCapsuleAugmentedContext` | collect | Собирает дополненный контекст (управление + RAG + Docs + сценарий + глоссарий) |
| `capsuleAiSmokeTest` | generate | Дымовой тест LLM-моста codebase с минимальным промптом |
| `generateCapsuleTranscript` | generate | Создаёт статью-транскрипт AsciiDoc из заметок докладчика |
| `generateCapsuleChapters` | generate | Маркеры глав + текстовые главы YouTube + описание |
| `generateCapsulePodcast` | generate | Подкаст только со звуком (склеенная MP3-озвучка) |
| `deployCapsule` | deploy | Перекадрирует капсулу в вертикаль 9:16 (TikTok/Shorts) через FFmpeg |
| `distributeCapsuleVideo` | distribute | Копирует итоговые видео в версионируемый каталог назначения |
| `collectCapsuleContext` / `transformCapsuleContext` / `collectCapsuleRetrieve` | collect/transform | Экспорт / разбор JSON контекста капсулы (контракт движка N3) |
| `scaffoldCapsuleContext` | generate | Создаёт закомментированный `capsule-context.yml` по умолчанию |
| `validateCapsuleVideoDuration` | verification | Сверяет длительность видео с суммой длительностей аудио TTS (opt-in) |
| `preflightCapsuleContent` | verification | Предварительная проверка контента до платного TTS (opt-in) |
| `renderQcBoards` | verification | Рендерит QC-планшеты (beats + «сказано vs показано») (opt-in) |
| `verifyCapsuleVoice` | verification | QC голоса через ASR — точность, дыхание, выбор дубля (opt-in) |
| `selectBestVoiceTake` | verification | Выбирает лучший дубль среди кандидатов слайда (opt-in) |
| `generateViralCampaign` | generate | Storyboard → короткая кампания (YouTube/TikTok) (opt-in) |
| `renderViralVariants` / `renderViralThumbnails` / `renderViralSubtitles` | generate | Рендерит вирусные варианты, миниатюры, караоке-субтитры (opt-in) |
| `validateViralCampaign` | verification | Проверяет манифест против созданных файлов (opt-in) |
| `normalizeViralStoryboard` | generate | Дополняет отсутствующие переходы storyboard (помощник авторства) |

> Опциональные (*opt-in*) возможности **отключены по умолчанию** (`false`) и никогда не
> блокируют основной конвейер.

## Конфигурация

Каждое значение разрешается из **4 источников** (приоритет **CLI > YAML > gradle.properties > ENV**):

```bash
./gradlew scaffoldCapsuleContext          # создаёт закомментированный capsule-context.yml
./gradlew generateCapsuleVideo -Pcapsule.tts.engine=espeak -Pcapsule.manim.quality=h
```

Представительный DSL (полный `CapsuleConfig` из 18 секций — см. [руководство по внутреннему
устройству](../capsule-plugin/README.md)):

```gradle
capsule {
    // Ввод
    outputDir.set("capsule")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")

    // TTS
    ttsEngine.set("piper")                 // "piper" (по умолчанию) или "espeak"
    ttsVoice.set("fr_FR-siwis-medium")
    piperExecutablePath.set("piper")
    ttsFallbackEnabled.set(true)
    espeakVoice.set("fr")
    espeakSpeed.set(150)
    ttsLanguage.set("fr")                  // 22 языка (LanguageCatalog)

    // Захват / вывод
    viewportWidth.set(1408)
    viewportHeight.set(792)
    captureStrategy.set("playwright")      // "playwright" | "screenshot" | "remotion"
    outputFormat.set("webm")               // "webm" (по умолчанию) | "mp4" | "both"
    outputBt709.set(false)                 // пометить MP4 как BT.709 (opt-in)

    // Субтитры
    subtitleEnabled.set(false)
    subtitleFormat.set("srt")              // "srt" или "vtt"
    subtitleBurnIn.set(false)

    // Дистрибуция
    ffmpegExecutablePath.set("ffmpeg")
    distribOutputWidth.set(1080)
    distribOutputHeight.set(1920)

    // Manim (опционально)
    manimExecutablePath.set("manim")
    manimQuality.set("l")                  // l, m, h, p, k
}
```

## Многоязычный конвейер

Одна исходная колода → одна локализованная капсула на каждый целевой язык. Конвейер
потребляет переведённые колоды, созданные `slider.translateDeck` (**22 языка**,
`LanguageCatalog` из N0-контракта `i18n-contracts`), и разрешает голос TTS для каждого языка
через чистый домен `capsule.multilang` (`MultiLanguageResolver` + `VoiceMapping`).

```
исходная колода (AsciiDoc)
        ↓ slider.translateDeck (LanguageCatalog, 22 языка)
<deckName>_<lang>-deck.html (по одной на язык)
        ↓ capsule.extractSpeakerNotes
<deckName>_<lang>-script.txt (по одному на язык)
        ↓ capsule.generateCapsuleVideoAllLanguages
<deckName>_<lang>.webm (одна локализованная капсула на язык)
```

Соглашение об именовании (пограничный контракт, согласован со slider):

- переведённая колода: `<deckName>_<lang>-deck.html`
- скрипт докладчика: `<deckName>_<lang>-script.txt`
- локализованное видео: `<deckName>_<lang>.webm`

```bash
./gradlew generateCapsuleVideoAllLanguages   # по одному видео на язык с парой колода+скрипт
./gradlew translateAndGenerateCapsuleVideos  # полная составная: translate → extract → видео
```

Язык без соответствующей пары колода + скрипт молча пропускается, и применяется правило
**Экономии чернил**: язык, чьё видео уже существует и валидно, никогда не перерисовывается.

## Опциональные возможности

- **Формат вывода** — `outputFormat = "mp4"` (или `"both"`) создаёт MP4/H.264 вдобавок к
  нативному WebM; `outputBt709 = true` помечает его как BT.709 (цветность вещания).
- **Пост-обработка звука** — фоновая музыка, громкость EBU R128 (измерение в 2 прохода) и
  sidechain ducking (`audioPost { … }`).
- **Субтитры** — генерация SRT/VTT и burn-in (`subtitleEnabled`, `subtitleBurnIn`).
- **Контроль качества (opt-in)** — предпроверка контента (`preflightCapsuleContent`),
  целостность видео через декодирование, детерминированная timeline, QC-планшеты
  (`renderQcBoards`) и QC голоса через ASR (`verifyCapsuleVoice`, `selectBestVoiceTake`).
- **Transcript** — обратный конвейер: статья AsciiDoc из заметок докладчика (`TEMPLATE`
  или `LLM`).
- **Podcast** — MP3 только со звуком озвучки.
- **Preview** — dry-run (`previewOnly = true`): рендерятся только PNG слайдов, mux пропускается.
- **Viral (opt-in)** — валидируемый storyboard → вертикальная короткая кампания 9:16 с
  hooks/CTA, привязанными к дополненному контексту, миниатюрами, караоке-субтитрами и
  `CampaignBundle`.

### Ритм слайдов (захват Remotion)

При `captureStrategy = "remotion"` колода анимируется, а не снимается: каждый блок слайда
появляется в ритме чтения, а не весь слайд сразу. Беats по умолчанию (секунды от начала
слайда): `.kicker` 0.0 · `h1`/`h2` 0.15 · `.rule` 0.5 · `.lede` 1.0 · `.stat` 4.5 ·
`.statlabel` 5.0. `.brand`, `.num` и `.grid` — это chrome (присутствуют с первого кадра).
Любой блок может задать свой beat через `data-at`:

```html
<p class="stat" data-at="8.5">542 076 km</p>
```

## Сборка и тесты (для контрибьюторов)

```bash
./gradlew build                    # полная сборка
./gradlew check                    # юнит + функциональные + Cucumber
./gradlew test                     # только юнит JUnit5
./gradlew cucumberTest -PrunCucumber
./gradlew functionalTest           # Gradle TestKit
./gradlew publishToMavenLocal      # локальная публикация
```

## Устранение неполадок

| Симптом | Решение |
|---------|---------|
| Chromium Playwright не найден | запустите один раз для автозагрузки или задайте `chromiumExecutablePath` |
| Piper TTS не создаёт звук | проверьте `piperExecutablePath`, убедитесь, что модель голоса установлена |
| `deployCapsule` завершается ошибкой | проверьте, что FFmpeg в `PATH` (`ffmpeg -version`) |
| Слайды Manim не отрендерены | задайте `manimExecutablePath` = `noop` для теста без Manim |
| Тайм-аут захвата видео | увеличьте `playwrightTimeout` (по умолчанию 120000 мс) |
| Недостаточно памяти | `export GRADLE_OPTS="-Xmx2g"` |

## Документация

- [capsule-plugin/README.md](../capsule-plugin/README.md) — внутреннее устройство плагина (руководство разработчика и контрибьютора)
- [capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) — архитектура конвейера (PlantUML)

## Лицензия

Apache License 2.0 — см. [LICENSE](../LICENSE).

---

_Часть экосистемы CCCP Education — `groupId: education.cccp`._
