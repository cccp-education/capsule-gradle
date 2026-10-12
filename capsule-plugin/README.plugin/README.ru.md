<!-- translated from README.md rev 0.0.8 -->
# capsule-gradle — Внутреннее устройство плагина

> Руководство для разработчиков и контрибьюторов плагина Gradle `capsule-plugin`.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.8&color=green)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![Покрытие](https://img.shields.io/static/v1?label=%D0%BF%D0%BE%D0%BA%D1%80%D1%8B%D1%82%D0%B8%D0%B5&message=Kover&color=blue)]()
[![Лицензия](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=%D0%9B%D0%B8%D1%86%D0%B5%D0%BD%D0%B7%D0%B8%D1%8F)](../../LICENSE)

- **Версия**: `0.0.8` (Maven Central) · **Группа**: `education.cccp` · **ID плагина**: `education.cccp.capsule`
- **Toolchain**: Java 25 · Kotlin 2.4.10 · Gradle 9.8.1
- **Сборка**: `./gradlew build -x test` · **Тесты**: `./gradlew check` (юнит + функциональные через TestKit; Cucumber через `-PrunCucumber`) · **Покрытие**: `./gradlew koverReport`

🌐 Языки: [English](../README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | **Русский** | [اردو](README.ur.md)

---

## Что делает

`capsule-gradle` превращает колоду reveal.js (созданную `slider-gradle`) в
синхронизированную видеокапсулу — озвучка TTS для каждого слайда, захват видео,
субтитры, конвертация формата, постобработка звука, контроль качества и
вертикальный короткий/вирусный конвейер — без ручной постпродакшн-работы.
Это плагин Gradle уровня N2 (его использует раннер N3, сам он N3 не является).

## Структура модуля

```
capsule-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml            # внутренний каталог версий
├── doc/CAPSULE_ARCHITECTURE.adoc        # архитектура конвейера (PlantUML)
└── src/
    ├── main/kotlin/capsule/
    │   ├── CapsulePlugin.kt             # точка входа — применяет slider, регистрирует задачи, сливает конфиг
    │   ├── CapsuleManager.kt            # регистрация задач + фабрики движков (resolve*)
    │   ├── CapsuleModels.kt             # DSL CapsuleExtension + CapsuleConventions
    │   ├── CapsuleConfig.kt             # неизменяемый конфиг — 18 секций
    │   ├── CapsuleConfigLoader.kt       # загрузчик YAML с подстановкой env ${VAR}
    │   ├── CapsuleConfigMerger.kt       # слияние 4 источников (CLI > YAML > gradle.properties > ENV)
    │   ├── CapsuleConfigLogger.kt       # структурированный лог конфига (CR-8)
    │   ├── Capsule{Messages}*.kt        # разрешение ресурсов i18n (10 языков)
    │   ├── TtsManager.kt                # интеграция TTS Piper + espeak
    │   ├── PlaywrightManager.kt         # захват Chromium headless
    │   ├── CaptureResolver.kt           # выбор PLAYWRIGHT / SCREENSHOT / REMOTION
    │   ├── ScreenshotPlanner.kt         # чистый план: PNG на слайд → WebM → concat demuxer
    │   ├── Remotion*.kt                 # стратегия захвата Remotion (Node/@remotion/renderer)
    │   ├── Manim{Engine,SlideReplacer,VideoMixer,ParallelRenderer}.kt  # обогащение Manim
    │   ├── Mp4ConversionCommand.kt      # чистый argv FFmpeg для WebM→MP4 (+ BT.709, opt-in)
    │   ├── VideoFormatConverter.kt      # транскодирование WebM → MP4/H.264 (CAP-MP4)
    │   ├── FormatConversion.kt          # выбор WEBM / MP4 / BOTH
    │   ├── VideoDestinationResolver.kt  # версионируемый приёмник (TIMESTAMP / INCREMENTAL)
    │   ├── Subtitle{BurnInCommand,BurnInService,Cartoner,TextNormalizer,
    │   │            TimingCalculator,TimingDistributor,Models}.kt + SrtGenerator.kt + VttGenerator.kt
    │   ├── Html{SectionParser,Escape}.kt
    │   ├── AudioConversionUtil.kt       # конвертация WAV→MP3
    │   ├── MediaProbeUtil.kt            # определение длительности через ffprobe
    │   ├── RenderSignature.kt           # подпись рендера + переиспользование .sig (PRT-06)
    │   ├── ProcessRunner.kt / FileReplace.kt / StrictModeGuard.kt
    │   ├── ai/            # мост LLM к LlmBuildService из codebase (CapsuleLlmService, capsuleAiSmokeTest)
    │   ├── anim/          # порт таймингов Remotion (RemotionTiming)
    │   ├── audio/         # CAP-AUDIO: AudioPostCommand (чистый argv), AudioPostProcessor (+NoOp), BGM/loudnorm/ducking
    │   ├── chapters/      # маркеры глав, CardRenderer, YouTubeChaptersFormatter
    │   ├── ci/            # CucumberTestGuard (CR-10), ProcessMigrationGuard, OssVocabularyGuard
    │   ├── context/       # дополненный контекст: CapsuleContext(Builder), загрузчики Doc/Scenario/Glossary, ProvenanceTracker
    │   ├── feed/          # контракт извлечения заметок докладчика (SLD-11) + составные регистраторы
    │   ├── i18n/          # CapsuleMessages (наборы для 10 языков)
    │   ├── multilang/     # CAP-29: MultiLanguageResolver, VoiceMapping, CapsuleVideoPlan(ner), раннер AllLanguages
    │   ├── pipeline/      # CAP-ARCH-3: граф koog CapsulePipelineGraph, CapsuleState, CapsuleLlm (+ChatModel), ContentPlanValidator
    │   ├── podcast/       # CAP-PODCAST: PodcastConcatenator (+Impl/NoOp), PodcastConcatCommand
    │   ├── preview/       # dry-run (capture.previewOnly — только PNG)
    │   ├── publish/       # VideoDescriptionBuilder (ограждение YouTube)
    │   ├── qc/            # CAP-QC: ContentPreflight (PRT-05), QcBoard/планировщики + RenderQcBoardsTask (PRT-08)
    │   ├── timeline/      # PRT-07: детерминированная timeline (целые кадры, lead/tail/avance)
    │   ├── transcript/    # CAP-TRANSCRIPT: TranscriptPlan/Builder, ChatModelTranscriptEnhancer
    │   ├── validation/    # VideoDurationValidator + VideoIntegrityChecker (PRT-04)
    │   ├── viral/         # CAP-VIRAL: storyboard, deck builder, campaign bundle/gate, thumbnail, караоке
    │   └── voice/         # CAP-QC-VOICE: TextNormalizer, WordAligner, FidelityScorer, SnrEstimator,
    │                      #   PronunciationLexicon, NumberSpeller, SpeechRecognizer (порт), TakeSelector
    ├── main/resources/capsule/
    │   ├── i18n/Messages[_xx].properties   # 10 языков + базовый
    │   └── remotion/                        # композиция Remotion (Capsule.jsx, Root.jsx, render.mjs)
    ├── test/kotlin/capsule/
    │   ├── scenarios/                       # определения шагов Cucumber + раннеры по фичам
    │   └── **/*Test.kt                      # юнит-тесты
    ├── test/features/                       # *.feature — сценарии Cucumber (44 файла)
    └── functionalTest/kotlin/capsule/       # функциональные тесты Gradle TestKit
```

## Задачи Gradle

| Группа | Задачи |
|--------|--------|
| Основной конвейер | `generateCapsuleScript`, `generateCapsule` (TTS), `generateCapsuleVideo` (вставка звука + захват), `generateCapsuleVideoAllLanguages`, `deployCapsule` (перекадрирование 9:16), `distributeCapsuleVideo` (версионируемый приёмник) |
| LLM-контент | `generateCapsuleContent`, `capsuleAiSmokeTest`, `collectCapsuleAugmentedContext`, `generateCapsuleContentAndVideos` |
| Контекст | `collectCapsuleContext`, `transformCapsuleContext`, `collectCapsuleRetrieve`, `scaffoldCapsuleContext` |
| Вывод | `generateCapsuleTranscript`, `generateCapsuleChapters`, `generateCapsulePodcast`, `validateCapsuleVideoDuration` |
| Качество (opt-in) | `preflightCapsuleContent`, `renderQcBoards`, `verifyCapsuleVoice`, `selectBestVoiceTake` |
| Виральное (opt-in) | `generateViralCampaign`, `normalizeViralStoryboard`, `renderViralVariants`, `renderViralThumbnails`, `renderViralSubtitles`, `validateViralCampaign` |
| Композит feed | `translateAndExtractSpeakerNotes`, `translateAndGenerateCapsuleVideos` |

## Зависимости

`capsule-gradle` использует `slider-gradle` (`education.cccp:slider:0.0.19`) через `compileOnly` —
контракт на основе файлов (capsule читает HTML колоды и текст скрипта, но никогда их не меняет).
`CapsulePlugin` автоматически применяет `education.cccp.slider` во время выполнения, если он доступен.

С CAP-ARCH-1 capsule также использует LLM-основу codebase
(`education.cccp:codebase-plugin`) как `implementation` — мост `LlmBuildService`
предоставляет `ChatModel`, используемый `generateCapsuleContent`, с рефолбэком mock-LLM через
`-Pollama.baseUrl` для тестов (ноль сети, ноль пула).

| Библиотека | Версия | Роль |
|------------|--------|------|
| **Playwright** | 1.52.0 | Chromium headless — управление reveal.js + захват видео (нативный JVM, без npm) |
| **koog-agents** | 1.3.0 | Kotlin DSL для оркестрации агентов (StateGraph / ConditionalEdges) — `CapsulePipelineGraph` |
| **langchain4j** | 1.22.0 | Адаптеры `ChatModel` (мост LLM, обогатитель транскрипта) — версия через BOM |
| **Jackson** | 2.21.2 | YAML-конфиг + сериализация JSON (databind, kotlin, dataformat-yaml) |
| **Kover** | 0.9.8 | Отчёты о покрытии (XML + HTML, включены в `check`) |
| **Cucumber** | 7.34.3 | BDD-тесты (cucumber-java, junit-platform-engine, picocontainer) |
| **Kotlin** | 2.4.10 | DSL плагина |
| **codebase-plugin** | 0.0.17 | LLM-основа N1 — мост `LlmBuildService` (`capsule.ai`) |
| **codebase-contracts** | 0.0.2 | Контракты N0 — CompositeContext / ContextChannel / ChannelBudget (`capsule.context`) |
| **i18n-contracts** | 0.0.4 | Контракты N0 — `LanguageCatalog` / `TranslationService` (`capsule.i18n`) |
| **slider** | 0.0.19 | compileOnly — колода reveal.js + исходник скрипта capsule |

Внешние инструменты (не Maven-зависимости): **Piper** (локальный офлайн TTS, по умолчанию),
**espeak-ng** (резервный TTS-движок), **FFmpeg** (перекадрирование, burn-in, MP4, аудио-пост),
**Manim** (опциональные анимированные схемы, `[manim:SceneName]`), **Chromium** (загружается
Playwright автоматически при первом запуске), **Node** (стратегия Remotion).

## Конфигурация

`CapsuleConfig` содержит **18 секций**: `input`, `tts`, `capture`, `distrib`, `manim`,
`output`, `strictMode`, `context`, `validation`, `qc`, `audioPost`, `transcript`,
`remotion`, `podcast`, `preview`, `chapters`, `viral`, `voice`.

Каждое значение разрешается из **4 источников** (приоритет **CLI > YAML > gradle.properties > ENV**):

```bash
# CLI
./gradlew generateCapsuleVideo -Poutput.format=mp4 -Pcapture.previewOnly=true

# gradle.properties / ENV
capsule.output.format=mp4
CAPSULE_OUTPUT_FORMAT=mp4
```

Новые возможности **opt-in (`false` по умолчанию)** для обратной совместимости.

## Матрица тестов

| Задача | Область | Детали |
|--------|---------|--------|
| `test` | Юнит-тесты JUnit5 | Исключает `capsule.scenarios.**`. Тесты `@integration` *не* фильтруются: они сами пропускаются, когда нет внешнего инструмента (Chromium, espeak, FFmpeg, Manim) |
| `cucumberTest` | Cucumber BDD | фичи в `src/test/features` (44 файла `.feature`), `forkEvery = 1`, `maxParallelForks = 1`, heap 1g. Пропускается, если нет `-PrunCucumber` или `CI=true` (CR-10) |
| `functionalTest` | Gradle TestKit | Функциональные тесты применения плагина |
| `check` | Всё | Зависит от `test` + `cucumberTest` + `functionalTest` |

Теги Cucumber: `@ai`, `@architecture`, `@audio`, `@boundary`, `@burnin`, `@capsule`, `@capture`, `@ci`, `@config`, `@context`, `@cross-borough`, `@distrib`, `@docs`, `@duration`, `@feed`, `@format`, `@glossary`, `@i18n`, `@integration`, `@logging`, `@manim`, `@model`, `@multilang`, `@no-yaml`, `@parallel`, `@pipeline`, `@post`, `@provenance`, `@rag`, `@scenario`, `@sequential`, `@strategy`, `@strict`, `@style`, `@subtitles`, `@timeout`, `@transcript`, `@translation`, `@tts`, `@validation`, `@versioning`, `@vtt`, `@wiring`. **38** выделенных задач `cucumberTest*` изолируют по одной фиче (`CucumberTaskSpec` в `build.gradle.kts`).

Базовая линия тестов: эталонный зелёный — workflow CI `Full Tests` на `main`
(юнит + функциональный TestKit + агрегированный/реальный рендер Cucumber; диагностический
`cucumberTest` пропускается по умолчанию с CR-10).

## LLM-конвейер контента (CAP-ARCH-3)

`generateCapsuleContent` оркеструет граф `CapsulePipelineGraph` (koog) за один вызов:

```
propose-context → validate-context → generate-speaker-notes
```

- **Входы**: исходная колода `.adoc` (`-Pdeck.file=<путь>` или первый `.adoc` в `slides/misc`),
  опциональный дополненный контекст от `collectCapsuleAugmentedContext`
  (`build/capsule/augmented-context.txt`), целевой язык (`-Pdeck.language`, по умолчанию `fr`).
- **Выходы** (объявлены → Экономия чернил через UP-TO-DATE): обогащённые заметки докладчика
  `build/capsule/<deckName>-speaker-notes.adoc` + TTS-скрипт `build/capsule/<deckName>-script.txt`.
- **LLM**: разрешается через `LlmBuildService` из codebase; тесты подставляют mock-сервер Ollama
  с `-Pollama.baseUrl=http://localhost:<port>` (маршрутизация по промпту: propose → JSON плана
  контента, generate → обогащённый AsciiDoc). Ноль сети, ноль пула в тестах.

```bash
./gradlew generateCapsuleContent \
  -Pdeck.file=slides/misc/my-deck.adoc \
  -Pdeck.language=en
```

## Настройка JVM

Тесты Cucumber используют `maxHeapSize = "1g"` с `forkEvery = 1` (новая JVM на сценарий)
и `maxParallelForks = 1` (последовательное выполнение для стабильности).

```bash
export GRADLE_OPTS="-Xmx2g"   # локальные сборки
```

## Команды сборки

```bash
./gradlew build                       # полная сборка (компиляция + тесты)
./gradlew build -x test               # только компиляция
./gradlew check                       # все тесты (юнит + Cucumber + функциональные)
./gradlew test                        # только юнит JUnit5
./gradlew cucumberTest -PrunCucumber  # Cucumber BDD (агрегированный раннер)
./gradlew functionalTest              # Gradle TestKit
./gradlew koverReport                 # отчёты о покрытии (XML + HTML)
./gradlew publishToMavenLocal         # локальная публикация
```

## CI-конвейер

Четыре workflow в `.github/workflows/`:

| Workflow | Триггер | Job |
|----------|---------|-----|
| `test.yml` — Fast Tests (гейт PR) | push / PR | JDK 25 + кэш Gradle → `./gradlew test` (юнит) |
| `full-tests.yml` — Full Tests | push | ffmpeg + espeak, **кэш Chromium Playwright**, кэш Gradle → `./gradlew test functionalTest`, затем `./gradlew cucumberTest cucumberTestViralRender -PrunCucumber` |
| `cucumber-full.yml` — диагностика | вручную (`workflow_dispatch`) | полный `cucumberTest` (не блокирующий) |
| `llm-smoke.yml` — Ollama smoke | вручную | кэш образа + модели Ollama, проверяет ответ модели |

Кэш: `gradle/actions/setup-gradle@v4` (wrapper + зависимости + build cache),
`.github/actions/setup-playwright` (Chromium, ключ из версии `playwright` каталога),
`.github/actions/setup-ollama` (образ + модели).

## Публикация (NMCP)

**Опубликовано в Maven Central** — версия `0.0.8` (предыдущие `0.0.7`, `0.0.6`, `0.0.5`, …).
POM настраивается через `maven-publish` и `signing`:

- POM: name "Capsule Gradle Plugin", description "Generation automatisee de capsules video
  depuis des decks reveal.js", URL `github.com/cheroliv/capsule-gradle/`
- Подпись: `useGpgCmd()` (выполняется при публикации)
- Агрегация NMCP: `publishAggregationToCentralPortal` (учётные данные в `~/.gradle/gradle.properties`)

```bash
./gradlew publishToMavenLocal
./gradlew publishAggregationToCentralPortal --no-daemon
```

## Документы по архитектуре и управлению

- [doc/CAPSULE_ARCHITECTURE.adoc](../doc/CAPSULE_ARCHITECTURE.adoc) — архитектура конвейера (PlantUML)
- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPIC, дорожная карта и панель управления
- [AGENT.adoc](../AGENT.adoc) — абсолютные правила (вкл. Закон экономии чернил)
- [BACKLOG.adoc](../BACKLOG.adoc) — детальный статус EPIC

Все EPIC закрыты: `CAP-VIRAL` (14/14), `CAP-SHORT` (6/6), `CAP-VIRAL-E2E` (3/3),
`CAP-CONTEXT` (4/4), `CAP-QC` (9/9), `CAP-QC-VOICE` (5/5), `LLM-RESILIENCE` (1/1),
`CAP-ARCH`, `CAP-29`, `CAP-MP4`, `CAP-AUDIO`, `CAP-TRANSCRIPT`, `CAP-PODCAST`,
`CAP-GLOSSARY`, `CAP-PROVENANCE`, `CAP-27`, `CAP-28`, `RENAME`, и набор ревью
CR-1→CR-10. Активный бэклог пуст (см. `.agents/INDEX.adoc`).

## Участие

1. Сборка компилируется: `./gradlew build -x test`
2. Все тесты зелёные: `./gradlew check` (или `-PrunCucumber` для BDD)
3. Соблюдайте абсолютные правила (см. [AGENT.adoc](../AGENT.adoc)):
   - Никаких commit/push без разрешения
   - DAG N2 — импортируется N3, никогда не импортирует N3
   - Capsule использует slider (только чтение, никогда не меняет исходную колоду)
   - Ноль секретов/токенов в коде
4. Соблюдайте Закон экономии чернил — никогда не повторяйте дорогую операцию
   (TTS, захват Playwright), если валидный артефакт для того же входа уже существует
5. Baby-step DDD/TDD/BDD — по одному файлу за раз; юнит → функциональный → Cucumber

## Лицензия

Apache License 2.0 — см. [LICENSE](../../LICENSE).

---

_Часть экосистемы CCCP Education — `groupId: education.cccp`._
