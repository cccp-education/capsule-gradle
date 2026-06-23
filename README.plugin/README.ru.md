<!-- translated from README.md rev 0.0.1-SNAPSHOT -->
# capsule-gradle — Внутреннее устройство плагина

> Руководство для разработчиков и контрибьюторов плагина Gradle `capsule-plugin`.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.1-SNAPSHOT&color=orange)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=coverage&message=Kover&color=blue)]()
[![License](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=License)](../LICENSE)

- **Версия**: `0.0.1-SNAPSHOT` (не опубликовано) · **Группа**: `education.cccp` · **ID плагина**: `education.cccp.capsule`
- **Toolchain**: Java 24 · Kotlin 2.3.20 · Gradle 9.5
- **Сборка**: `./gradlew build -x test` · **Тесты**: `./gradlew check` (195 unit + 22 Cucumber PASS) · **Покрытие**: `./gradlew koverReport`

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | **Русский** | [اردو](README.ur.md)

---

## Структура модулей

```
capsule-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml          # каталог версий
├── doc/
│   └── CAPSULE_ARCHITECTURE.adoc      # архитектура пайплайна (PlantUML)
└── src/
    ├── main/kotlin/capsule/
    │   ├── CapsulePlugin.kt           # точка входа — применяет slider, регистрирует задачи, слияние конфига
    │   ├── CapsuleManager.kt          # регистрация задач + парсинг скрипта + фабричные методы
    │   ├── CapsuleModels.kt           # DSL CapsuleExtension + CapsuleConventions + модели данных
    │   ├── CapsuleConfig.kt           # неизменяемый конфиг (5 секций: input, tts, capture, distrib, manim)
    │   ├── CapsuleConfigLoader.kt     # YAML-загрузчик с разрешением env ${VAR}
    │   ├── CapsuleConfigMerger.kt     # слияние 4 источников (CLI > YAML > props > ENV)
    │   ├── CapsuleScriptTask.kt       # generateCapsuleScript
    │   ├── CapsuleBuildTask.kt        # generateCapsule (синтез TTS)
    │   ├── CapsuleVideoTask.kt        # generateCapsuleVideo (инъекция аудио + захват Playwright)
    │   ├── CapsuleDistribTask.kt     # deployCapsule (обрезка 9:16 FFmpeg)
    │   ├── CapsuleCompositeContextTask.kt  # collectCapsuleContext (экспорт JSON N3)
    │   ├── CapsuleParseContextTask.kt # transformCapsuleContext + collectCapsuleRetrieve
    │   ├── CapsuleScaffoldTask.kt    # scaffoldCapsuleContext
    │   ├── TtsManager.kt             # интеграция TTS Piper + espeak
    │   ├── PlaywrightManager.kt      # захват Chromium headless
    │   ├── ManimEngine.kt            # рендеринг Manim (NoOp/Impl)
    │   ├── ManimSlideReplacer.kt     # замена HTML→видео встраивания
    │   ├── ManimVideoMixer.kt        # мультиплексирование MP4 Manim + аудио TTS
    │   ├── ManimParallelRenderer.kt  # параллельный рендеринг Manim
    │   ├── SubtitleModels.kt         # модели данных субтитров
    │   ├── SubtitleTimingCalculator.kt
    │   ├── SrtGenerator.kt           # генерация субтитров SRT
    │   ├── VttGenerator.kt           # генерация субтитров VTT
    │   ├── SubtitleBurnInService.kt  # вшитие субтитров FFmpeg
    │   ├── HtmlSectionParser.kt       # парсинг HTML презентации reveal.js
    │   ├── AudioConversionUtil.kt    # конвертация WAV→MP3
    │   ├── MediaProbeUtil.kt         # зондирование длительности ffprobe
    │   └── LanguageVoiceMapping.kt    # сопоставление голосов FR/EN/ES/DE
    ├── test/kotlin/capsule/
    │   ├── CapsulePluginTest.kt
    │   ├── scenarios/                 # Cucumber BDD step definitions + runners
    │   │   ├── CapsuleStepDefinitions.kt
    │   │   ├── CucumberTestRunner.kt
    │   │   └── CucumberIntegrationTestRunner.kt
    │   └── *Test.kt                   # 17 классов unit-тестов
    ├── test/features/
    │   └── capsule.feature            # 22 сценария Cucumber
    └── functionalTest/kotlin/capsule/
        └── CapsulePluginFunctionalTest.kt
```

## Зависимость N2

`capsule-gradle` потребляет `slider-gradle` (`education.cccp:slider:0.0.6`) через `compileOnly`.
Контракт основан на файлах — capsule читает HTML презентации и текстовые файлы скриптов,
созданные slider, и никогда их не изменяет. CapsulePlugin автоматически применяет
`education.cccp.slider` при выполнении, если доступно.

## Ключевые библиотеки

| Библиотека | Версия | Роль |
|---------|---------|------|
| **Playwright** | 1.52.0 | Chromium headless — управление reveal.js + захват видео (JVM native, без npm) |
| **koog-agents** | 0.8.0 | Kotlin DSL для оркестрации агентов (StateGraph / ConditionalEdges) |
| **Jackson** | 2.18.3 | YAML-конфиг + JSON-сериализация (databind, kotlin, dataformat-yaml) |
| **Kover** | 0.9.8 | Отчёты покрытия (XML + HTML, встроены в `check`) |
| **Cucumber** | 7.34.3 | BDD-тесты (cucumber-java, junit-platform-engine, picocontainer) |
| **Kotlin** | 2.3.20 | DSL плагина |
| **slider** | 0.0.6 | compileOnly — презентация reveal.js + источник скрипта капсулы |

Внешние инструменты (не Maven-зависимости):
- **Piper** — локальный офлайн TTS-движок (по умолчанию)
- **espeak** — резервный TTS-движок
- **FFmpeg** — обрезка 9:16 (`deployCapsule`) и вшитие субтитров
- **Manim** — рендеринг математической анимации (опционально, `[manim:SceneName]` в скрипте)
- **Chromium** — автозагружается Playwright при первом запуске

## Матрица тестов

| Задача | Область | Детали |
|------|-------|---------|
| `test` | JUnit5 unit-тесты | Исключает тег `@integration` и `capsule.scenarios.**` |
| `cucumberTest` | Cucumber BDD | 22 сценария, `forkEvery = 1`, `maxParallelForks = 1`, heap 1g |
| `functionalTest` | Gradle TestKit | Функциональные тесты применения плагина |
| `check` | Все | Зависит от `test` + `cucumberTest` + `functionalTest` |

Теги Cucumber: `@integration`, `@manim`, `@config`, `@tts`, `@subtitles`, `@burnin`, `@style`, `@parallel`。

Всего тестов: **195 unit + 22 Cucumber = 217 PASS** (базлайн сессии 029)。

## Настройка JVM

Тесты Cucumber используют `maxHeapSize = "1g"` с `forkEvery = 1` (свежая JVM на каждый сценарий)
и `maxParallelForks = 1` (последовательное выполнение для стабильности).

Для локальных сборок:
```bash
export GRADLE_OPTS="-Xmx2g"
```

## Команды сборки

```bash
./gradlew build                       # полная сборка (компиляция + тесты)
./gradlew build -x test               # только компиляция
./gradlew check                       # все тесты (unit + Cucumber + функциональные)
./gradlew test                        # только JUnit5 unit
./gradlew cucumberTest                # Cucumber BDD
./gradlew functionalTest              # Gradle TestKit
./gradlew koverReport                 # отчёты покрытия (XML + HTML)
./gradlew publishToMavenLocal         # локальная публикация
```

## CI-пайплайн

`.github/workflows/test.yml` определяет одну задачу:
1. **Build & Test** — `./gradlew check` при каждом push/PR в main/master (≤ 15 мин, JDK 24 Temurin, action настройки Gradle)

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

## Публикация (NMCP)

**Не опубликовано** — версия `0.0.1-SNAPSHOT`. POM настроен для Maven Central через
плагины `maven-publish` и `signing`:

- POM: имя "Capsule Gradle Plugin", описание "Generation automatisee de capsules video
  pedagogiques depuis des decks reveal.js", URL `github.com/cheroliv/capsule-gradle/`
- Подпись: `useGpgCmd()` (настроено, но пока не выполнялось)
- Цель публикации: `publishToMavenLocal` доступен; NMCP `publishAggregationToCentralPortal`
  требует учётных данных в `~/.gradle/gradle.properties`

Для публикации, когда будет готово:
```bash
./gradlew publishAggregationToCentralPortal --no-daemon
```

## Документация по архитектуре

- [CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) — Архитектура
  пайплайна с диаграммами PlantUML (глобальный пайплайн, компоненты, последовательность, развёртывание)
- [.agents/INDEX.adoc](../capsule-plugin/.agents/INDEX.adoc) — EPIC, roadmap и управление
- [AGENT.adoc](../capsule-plugin/AGENT.adoc) — Абсолютные правила (7 правил, Закон экономии чернил)
- [CODE_REVIEW.adoc](../capsule-plugin/CODE_REVIEW.adoc) — Code review (10 действенных EPIC CR-1→CR-10)

## Статус EPIC

CAP-0 до CAP-25 завершены. Активные EPIC (см. `.agents/INDEX.adoc`):

| EPIC | Описание | Статус |
|------|-------------|--------|
| CR-1 | Рефакторинг `CapsuleVideoTask.execute()` (218→76 строк) | ✅ Завершено (сессия 040) |
| CR-2 | Потокобезопасность + таймаут `captureSlideParallel` | ⬜ TODO |
| CR-3 | Обработка ошибок TTS — деградация сигнализируется | ⬜ TODO |
| CR-4 | Безопасность HTML escape — `injectSubtitleTrack` | ⬜ TODO |
| CR-5 | DRY `CapsuleConfigMerger` (~76 строк дублирования) | ⬜ TODO |
| CAP-27 | Вшитие VTT (пока только SRT) | ⬜ TODO |
| CAP-28 | Пробелы покрытия — PiperTtsEngine 31%, ManimEngineImpl 25% | ⬜ TODO |

## Содействие

1. Сборка компилируется: `./gradlew build -x test`
2. Все тесты зелёные: `./gradlew check`
3. Следовать 7 абсолютным правилам (см. [AGENT.adoc](../capsule-plugin/AGENT.adoc)):
   - Без коммитов/пушей без разрешения
   - DAG N2 — импортируется N3, никогда не импортирует N3
   - Capsule потребляет slider (только чтение, никогда не изменяет исходную презентацию)
   - Ноль секретов/токенов в коде
4. Соблюдать Закон экономии чернил — никогда не повторять дорогостоящие операции (TTS, захват Playwright)
   когда для тех же входных данных уже существует валидный артефакт

## Лицензия

Apache License 2.0 — см. [LICENSE](../LICENSE)。

---

_Часть экосистемы CCCP Education — `groupId: education.cccp`._