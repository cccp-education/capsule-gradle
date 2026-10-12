<!-- translated from README.md rev 0.0.10 -->
# capsule-gradle — Interioridades del Plugin

> Guía para desarrolladores y contribuidores del plugin Gradle `capsule-plugin`.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.10&color=green)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![Cobertura](https://img.shields.io/static/v1?label=cobertura&message=Kover&color=blue)]()
[![Licencia](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=Licencia)](../../LICENSE)

- **Versión**: `0.0.10` (Maven Central) · **Grupo**: `education.cccp` · **ID del plugin**: `education.cccp.capsule`
- **Toolchain**: Java 25 · Kotlin 2.4.10 · Gradle 9.8.1
- **Build**: `./gradlew build -x test` · **Pruebas**: `./gradlew check` (unitarias + funcionales vía TestKit; Cucumber con `-PrunCucumber`) · **Cobertura**: `./gradlew koverReport`

🌐 Idiomas: [English](../README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | **Español** | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Qué hace

`capsule-gradle` convierte un deck reveal.js (producido por `slider-gradle`) en una
cápsula de vídeo sincronizada — locución TTS por diapositiva, captura de vídeo,
subtítulos, conversión de formato, postproducción de audio, control de calidad y un
pipeline vertical corto/viral — sin postproducción humana. Es un plugin Gradle N2
(lo consume el runner N3, nunca es N3).

## Estructura del módulo

```
capsule-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml            # catálogo de versiones interno
├── doc/CAPSULE_ARCHITECTURE.adoc        # arquitectura del pipeline (PlantUML)
└── src/
    ├── main/kotlin/capsule/
    │   ├── CapsulePlugin.kt             # punto de entrada — aplica slider, registra tareas, fusiona la config
    │   ├── CapsuleManager.kt            # registro de tareas + fábricas de motores (resolve*)
    │   ├── CapsuleModels.kt             # DSL CapsuleExtension + CapsuleConventions
    │   ├── CapsuleConfig.kt             # config inmutable — 18 secciones
    │   ├── CapsuleConfigLoader.kt       # cargador YAML con resolución de env ${VAR}
    │   ├── CapsuleConfigMerger.kt       # fusión de 4 fuentes (CLI > YAML > gradle.properties > ENV)
    │   ├── CapsuleConfigLogger.kt       # registro estructurado de la config (CR-8)
    │   ├── Capsule{Messages}*.kt        # resolución de recursos i18n (10 idiomas)
    │   ├── TtsManager.kt                # integración TTS Piper + espeak + Fish Audio
    │   ├── PlaywrightManager.kt         # captura Chromium headless
    │   ├── CaptureResolver.kt           # despacho PLAYWRIGHT / SCREENSHOT / REMOTION
    │   ├── ScreenshotPlanner.kt         # plan puro: PNG por diapositiva → WebM → concat demuxer
    │   ├── Remotion*.kt                 # estrategia de captura Remotion (Node/@remotion/renderer)
    │   ├── Manim{Engine,SlideReplacer,VideoMixer,ParallelRenderer}.kt  # enriquecimiento Manim
    │   ├── Mp4ConversionCommand.kt      # argv FFmpeg puro para WebM→MP4 (+ BT.709, opt-in)
    │   ├── VideoFormatConverter.kt      # transcodificación WebM → MP4/H.264 (CAP-MP4)
    │   ├── FormatConversion.kt          # despacho WEBM / MP4 / BOTH
    │   ├── VideoDestinationResolver.kt  # destino versionado (TIMESTAMP / INCREMENTAL)
    │   ├── Subtitle{BurnInCommand,BurnInService,Cartoner,TextNormalizer,
    │   │            TimingCalculator,TimingDistributor,Models}.kt + SrtGenerator.kt + VttGenerator.kt
    │   ├── Html{SectionParser,Escape}.kt
    │   ├── AudioConversionUtil.kt       # conversión WAV→MP3
    │   ├── MediaProbeUtil.kt            # sondeo de duración vía ffprobe
    │   ├── RenderSignature.kt           # firma de render + reutilización .sig (PRT-06)
    │   ├── ProcessRunner.kt / FileReplace.kt / StrictModeGuard.kt
    │   ├── ai/            # puente LLM hacia el LlmBuildService de codebase (CapsuleLlmService, capsuleAiSmokeTest)
    │   ├── anim/          # port del timing Remotion (RemotionTiming)
    │   ├── audio/         # CAP-AUDIO: AudioPostCommand (argv puro), AudioPostProcessor (+NoOp), BGM/loudnorm/ducking
    │   ├── chapters/      # marcadores de capítulo, CardRenderer, YouTubeChaptersFormatter
    │   ├── ci/            # CucumberTestGuard (CR-10), ProcessMigrationGuard, OssVocabularyGuard
    │   ├── context/       # contexto aumentado: CapsuleContext(Builder), loaders Doc/Scenario/Glossary, ProvenanceTracker
    │   ├── feed/          # contrato de extracción de notas del orador (SLD-11) + registrars compuestos
    │   ├── i18n/          # CapsuleMessages (bundles de 10 idiomas)
    │   ├── multilang/     # CAP-29: MultiLanguageResolver, VoiceMapping, CapsuleVideoPlan(ner), runner AllLanguages
    │   ├── pipeline/      # CAP-ARCH-3: grafo koog CapsulePipelineGraph, CapsuleState, CapsuleLlm (+ChatModel), ContentPlanValidator
    │   ├── podcast/       # CAP-PODCAST: PodcastConcatenator (+Impl/NoOp), PodcastConcatCommand
    │   ├── preview/       # dry-run (capture.previewOnly — solo PNG)
    │   ├── publish/       # VideoDescriptionBuilder (guarda de YouTube)
    │   ├── qc/            # CAP-QC: ContentPreflight (PRT-05), QcBoard/planificadores + RenderQcBoardsTask (PRT-08)
    │   ├── timeline/      # PRT-07: timeline determinista (fotogramas enteros, lead/tail/avance)
    │   ├── transcript/    # CAP-TRANSCRIPT: TranscriptPlan/Builder, ChatModelTranscriptEnhancer
    │   ├── validation/    # VideoDurationValidator + VideoIntegrityChecker (PRT-04)
    │   ├── viral/         # CAP-VIRAL: storyboard, deck builder, campaign bundle/gate, thumbnail, karaoke
    │   └── voice/         # CAP-QC-VOICE: TextNormalizer, WordAligner, FidelityScorer, SnrEstimator,
    │                      #   PronunciationLexicon, NumberSpeller, SpeechRecognizer (puerto), TakeSelector
    ├── main/resources/capsule/
    │   ├── i18n/Messages[_xx].properties   # 10 idiomas + base
    │   └── remotion/                        # composición Remotion (Capsule.jsx, Root.jsx, render.mjs)
    ├── test/kotlin/capsule/
    │   ├── scenarios/                       # definiciones de steps Cucumber + runners por feature
    │   └── **/*Test.kt                      # pruebas unitarias
    ├── test/features/                       # *.feature — escenarios Cucumber (44 archivos)
    └── functionalTest/kotlin/capsule/       # pruebas funcionales Gradle TestKit
```

## Tareas Gradle

| Grupo | Tareas |
|-------|--------|
| Pipeline principal | `generateCapsuleScript`, `generateCapsule` (TTS), `generateCapsuleVideo` (inyección de audio + captura), `generateCapsuleVideoAllLanguages`, `deployCapsule` (recorte 9:16), `distributeCapsuleVideo` (destino versionado) |
| Contenido LLM | `generateCapsuleContent`, `capsuleAiSmokeTest`, `collectCapsuleAugmentedContext`, `generateCapsuleContentAndVideos` |
| Contexto | `collectCapsuleContext`, `transformCapsuleContext`, `collectCapsuleRetrieve`, `scaffoldCapsuleContext` |
| Salida | `generateCapsuleTranscript`, `generateCapsuleChapters`, `generateCapsulePodcast`, `validateCapsuleVideoDuration` |
| Calidad (opt-in) | `preflightCapsuleContent`, `renderQcBoards`, `verifyCapsuleVoice`, `selectBestVoiceTake` |
| Viral (opt-in) | `generateViralCampaign`, `normalizeViralStoryboard`, `renderViralVariants`, `renderViralThumbnails`, `renderViralSubtitles`, `validateViralCampaign` |
| Compuesto feed | `translateAndExtractSpeakerNotes`, `translateAndGenerateCapsuleVideos` |

## Dependencias

`capsule-gradle` consume `slider-gradle` (`education.cccp:slider:0.0.19`) con `compileOnly` —
un contrato basado en archivos (capsule lee el HTML del deck y el texto del script, nunca los modifica).
`CapsulePlugin` aplica automáticamente `education.cccp.slider` en tiempo de ejecución si está disponible.

Desde CAP-ARCH-1, capsule también consume el socle LLM de codebase
(`education.cccp:codebase-plugin`) como `implementation` — el puente `LlmBuildService`
proporciona el `ChatModel` usado por `generateCapsuleContent`, con un repliegue mock-LLM vía
`-Pollama.baseUrl` para las pruebas (cero red, cero pool).

| Biblioteca | Versión | Rol |
|------------|---------|-----|
| **Playwright** | 1.52.0 | Chromium headless — control de reveal.js + captura de vídeo (JVM nativo, cero npm) |
| **koog-agents** | 1.3.0 | DSL Kotlin para orquestación de agentes (StateGraph / ConditionalEdges) — `CapsulePipelineGraph` |
| **langchain4j** | 1.22.0 | Adaptadores `ChatModel` (puente LLM, enriquecedor de transcripción) — versión vía BOM |
| **Jackson** | 2.21.2 | Config YAML + serialización JSON (databind, kotlin, dataformat-yaml) |
| **Kover** | 0.9.8 | Informes de cobertura (XML + HTML, integrados en `check`) |
| **Cucumber** | 7.34.3 | Pruebas BDD (cucumber-java, junit-platform-engine, picocontainer) |
| **Kotlin** | 2.4.10 | DSL del plugin |
| **codebase-plugin** | 0.0.17 | Socle LLM N1 — puente `LlmBuildService` (`capsule.ai`) |
| **codebase-contracts** | 0.0.2 | Contratos N0 — CompositeContext / ContextChannel / ChannelBudget (`capsule.context`) |
| **i18n-contracts** | 0.0.4 | Contratos N0 — `LanguageCatalog` / `TranslationService` (`capsule.i18n`) |
| **slider** | 0.0.19 | compileOnly — deck reveal.js + fuente del script de capsule |

Herramientas externas (no son dependencias Maven): **Piper** (TTS local sin conexión, por defecto),
**espeak-ng** (motor TTS de repliegue), **FFmpeg** (recorte, burn-in, MP4, audio post),
**Manim** (esquemas animados opcionales, `[manim:SceneName]`), **Chromium** (descargado
automáticamente por Playwright en la primera ejecución), **Node** (estrategia Remotion).

## Configuración

`CapsuleConfig` tiene **18 secciones**: `input`, `tts`, `capture`, `distrib`, `manim`,
`output`, `strictMode`, `context`, `validation`, `qc`, `audioPost`, `transcript`,
`remotion`, `podcast`, `preview`, `chapters`, `viral`, `voice`.

Cada valor se resuelve desde **4 fuentes** (precedencia **CLI > YAML > gradle.properties > ENV**):

```bash
# CLI
./gradlew generateCapsuleVideo -Poutput.format=mp4 -Pcapture.previewOnly=true

# gradle.properties / ENV
capsule.output.format=mp4
CAPSULE_OUTPUT_FORMAT=mp4
```

Las nuevas capacidades son **opt-in (`false` por defecto)** para compatibilidad retroactiva.

## Matriz de pruebas

| Tarea | Alcance | Detalles |
|-------|---------|----------|
| `test` | Pruebas unitarias JUnit5 | Excluye `capsule.scenarios.**`. Las pruebas `@integration` *no* se filtran: se auto-saltan cuando falta la herramienta externa (Chromium, espeak, FFmpeg, Manim) |
| `cucumberTest` | Cucumber BDD | features bajo `src/test/features` (44 archivos `.feature`), `forkEvery = 1`, `maxParallelForks = 1`, heap 1g. Se salta salvo `-PrunCucumber` o `CI=true` (CR-10) |
| `functionalTest` | Gradle TestKit | Pruebas funcionales de aplicación del plugin |
| `check` | Todo | Depende de `test` + `cucumberTest` + `functionalTest` |

Etiquetas Cucumber: `@ai`, `@architecture`, `@audio`, `@boundary`, `@burnin`, `@capsule`, `@capture`, `@ci`, `@config`, `@context`, `@cross-borough`, `@distrib`, `@docs`, `@duration`, `@feed`, `@format`, `@glossary`, `@i18n`, `@integration`, `@logging`, `@manim`, `@model`, `@multilang`, `@no-yaml`, `@parallel`, `@pipeline`, `@post`, `@provenance`, `@rag`, `@scenario`, `@sequential`, `@strategy`, `@strict`, `@style`, `@subtitles`, `@timeout`, `@transcript`, `@translation`, `@tts`, `@validation`, `@versioning`, `@vtt`, `@wiring`. **38** tareas `cucumberTest*` dedicadas aíslan una feature cada una (`CucumberTaskSpec` en `build.gradle.kts`).

Base de pruebas: el verde de referencia es el workflow CI `Full Tests` en `main`
(unitarias + TestKit funcional + Cucumber agregado/render real; el `cucumberTest`
de diagnóstico se salta por defecto desde CR-10).

## Pipeline de contenido LLM (CAP-ARCH-3)

`generateCapsuleContent` orquesta el `CapsulePipelineGraph` de koog en una sola invocación:

```
propose-context → validate-context → generate-speaker-notes
```

- **Entradas**: deck fuente `.adoc` (`-Pdeck.file=<ruta>` o el primer `.adoc` en `slides/misc`),
  contexto aumentado opcional de `collectCapsuleAugmentedContext`
  (`build/capsule/augmented-context.txt`), idioma destino (`-Pdeck.language`, por defecto `fr`).
- **Salidas** (declaradas → Economía de Tinta vía UP-TO-DATE): notas del orador enriquecidas
  `build/capsule/<deckName>-speaker-notes.adoc` + script TTS `build/capsule/<deckName>-script.txt`.
- **LLM**: resuelto vía el `LlmBuildService` de codebase; las pruebas inyectan un servidor Ollama
  simulado con `-Pollama.baseUrl=http://localhost:<port>` (enrutado por prompt: propose → JSON de
  plan de contenido, generate → AsciiDoc enriquecido). Cero red, cero pool en las pruebas.

```bash
./gradlew generateCapsuleContent \
  -Pdeck.file=slides/misc/my-deck.adoc \
  -Pdeck.language=en
```

## Ajuste de JVM

Las pruebas Cucumber usan `maxHeapSize = "1g"` con `forkEvery = 1` (JVM nueva por escenario)
y `maxParallelForks = 1` (ejecución secuencial para estabilidad).

```bash
export GRADLE_OPTS="-Xmx2g"   # builds locales
```

## Comandos de build

```bash
./gradlew build                       # build completo (compila + pruebas)
./gradlew build -x test               # solo compilación
./gradlew check                       # todas las pruebas (unitarias + Cucumber + funcionales)
./gradlew test                        # solo unitarias JUnit5
./gradlew cucumberTest -PrunCucumber  # Cucumber BDD (runner agregado)
./gradlew functionalTest              # Gradle TestKit
./gradlew koverReport                 # informes de cobertura (XML + HTML)
./gradlew publishToMavenLocal         # publicación local
```

## Pipeline CI

Cuatro workflows bajo `.github/workflows/`:

| Workflow | Disparador | Job |
|----------|-----------|-----|
| `test.yml` — Fast Tests (puerta PR) | push / PR | JDK 25 + Gradle en caché → `./gradlew test` (unitarias) |
| `full-tests.yml` — Full Tests | push | ffmpeg + espeak, **Chromium Playwright en caché**, Gradle en caché → `./gradlew test functionalTest`, luego `./gradlew cucumberTest cucumberTestViralRender -PrunCucumber` |
| `cucumber-full.yml` — diagnóstico | manual (`workflow_dispatch`) | `cucumberTest` completo (no bloqueante) |
| `llm-smoke.yml` — Ollama smoke | manual | imagen + modelo Ollama en caché, comprueba que el modelo responde |

Caché: `gradle/actions/setup-gradle@v4` (wrapper + dependencias + build cache),
`.github/actions/setup-playwright` (Chromium, clave derivada de la versión `playwright` del
catálogo), `.github/actions/setup-ollama` (imagen + modelos).

## Publicación (NMCP)

**Publicado en Maven Central** — versión `0.0.10` (anteriores `0.0.7`, `0.0.6`, `0.0.5`, …).
El POM se configura con `maven-publish` y `signing`:

- POM: nombre "Capsule Gradle Plugin", descripción "Generation automatisee de capsules video
  depuis des decks reveal.js", URL `github.com/cheroliv/capsule-gradle/`
- Firma: `useGpgCmd()` (ejecutada en la publicación)
- Agregación NMCP: `publishAggregationToCentralPortal` (credenciales en `~/.gradle/gradle.properties`)

```bash
./gradlew publishToMavenLocal
./gradlew publishAggregationToCentralPortal --no-daemon
```

## Documentos de arquitectura y gobernanza

- [doc/CAPSULE_ARCHITECTURE.adoc](../doc/CAPSULE_ARCHITECTURE.adoc) — arquitectura del pipeline (PlantUML)
- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs, roadmap y panel de gobernanza
- [AGENT.adoc](../AGENT.adoc) — reglas absolutas (incl. la Ley de la Economía de Tinta)
- [BACKLOG.adoc](../BACKLOG.adoc) — estado detallado de los EPICs

Todos los EPICs están cerrados: `CAP-VIRAL` (14/14), `CAP-SHORT` (6/6), `CAP-VIRAL-E2E` (3/3),
`CAP-CONTEXT` (4/4), `CAP-QC` (9/9), `CAP-QC-VOICE` (5/5), `LLM-RESILIENCE` (1/1),
`CAP-ARCH`, `CAP-29`, `CAP-MP4`, `CAP-AUDIO`, `CAP-TRANSCRIPT`, `CAP-PODCAST`,
`CAP-GLOSSARY`, `CAP-PROVENANCE`, `CAP-27`, `CAP-28`, `RENAME`, y el conjunto de revisión
CR-1→CR-10. El backlog accionable está vacío (ver `.agents/INDEX.adoc`).

## Contribuir

1. El build compila: `./gradlew build -x test`
2. Todas las pruebas verdes: `./gradlew check` (o `-PrunCucumber` para BDD)
3. Respetar las reglas absolutas (ver [AGENT.adoc](../AGENT.adoc)):
   - Sin commits/push sin permiso
   - DAG N2 — importable por N3, nunca importa N3
   - Capsule consume slider (solo lectura, nunca modifica el deck fuente)
   - Cero secretos/tokens en el código
4. Respetar la Ley de la Economía de Tinta — nunca re-ejecutar una operación costosa
   (TTS, captura Playwright) cuando ya existe un artefacto válido para la misma entrada
5. Baby-step DDD/TDD/BDD — un archivo a la vez; unitaria → funcional → Cucumber

## Licencia

Apache License 2.0 — ver [LICENSE](../../LICENSE).

---

_Parte del ecosistema CCCP Education — `groupId: education.cccp`._
