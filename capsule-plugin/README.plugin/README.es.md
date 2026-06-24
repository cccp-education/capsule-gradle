<!-- translated from README.md rev 0.0.1-SNAPSHOT -->
# capsule-gradle — Interior del Plugin

> Guía para desarrolladores y contribuyentes del plugin Gradle `capsule-plugin`.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.1-SNAPSHOT&color=orange)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=coverage&message=Kover&color=blue)]()
[![License](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=License)](../LICENSE)

- **Versión**: `0.0.1-SNAPSHOT` (no publicado) · **Grupo**: `education.cccp` · **ID del plugin**: `education.cccp.capsule`
- **Toolchain**: Java 24 · Kotlin 2.3.20 · Gradle 9.5
- **Build**: `./gradlew build -x test` · **Tests**: `./gradlew check` (195 unitarios + 22 Cucumber PASS) · **Cobertura**: `./gradlew koverReport`

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | **Español** | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Disposición de módulos

```
capsule-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml          # catálogo de versiones
├── doc/
│   └── CAPSULE_ARCHITECTURE.adoc      # arquitectura del pipeline (PlantUML)
└── src/
    ├── main/kotlin/capsule/
    │   ├── CapsulePlugin.kt           # punto de entrada — aplica slider, registra tareas, fusión de config
    │   ├── CapsuleManager.kt          # registro de tareas + parsing de script + métodos factory
    │   ├── CapsuleModels.kt           # DSL CapsuleExtension + CapsuleConventions + modelos de datos
    │   ├── CapsuleConfig.kt           # config inmutable (5 secciones: input, tts, capture, distrib, manim)
    │   ├── CapsuleConfigLoader.kt     # cargador YAML con resolución de env ${VAR}
    │   ├── CapsuleConfigMerger.kt     # fusión de 4 fuentes (CLI > YAML > props > ENV)
    │   ├── CapsuleScriptTask.kt       # generateCapsuleScript
    │   ├── CapsuleBuildTask.kt        # generateCapsule (síntesis TTS)
    │   ├── CapsuleVideoTask.kt        # generateCapsuleVideo (inyección de audio + captura Playwright)
    │   ├── CapsuleDistribTask.kt     # deployCapsule (recorte 9:16 FFmpeg)
    │   ├── CapsuleCompositeContextTask.kt  # collectCapsuleContext (export JSON N3)
    │   ├── CapsuleParseContextTask.kt # transformCapsuleContext + collectCapsuleRetrieve
    │   ├── CapsuleScaffoldTask.kt    # scaffoldCapsuleContext
    │   ├── TtsManager.kt             # integración TTS Piper + espeak
    │   ├── PlaywrightManager.kt      # captura Chromium headless
    │   ├── ManimEngine.kt            # renderizado Manim (NoOp/Impl)
    │   ├── ManimSlideReplacer.kt     # reemplazo HTML→vídeo embed
    │   ├── ManimVideoMixer.kt        # mux MP4 Manim + audio TTS
    │   ├── ManimParallelRenderer.kt  # renderizado Manim paralelo
    │   ├── SubtitleModels.kt         # modelos de datos de subtítulos
    │   ├── SubtitleTimingCalculator.kt
    │   ├── SrtGenerator.kt           # generación de subtítulos SRT
    │   ├── VttGenerator.kt           # generación de subtítulos VTT
    │   ├── SubtitleBurnInService.kt  # grabado de subtítulos FFmpeg
    │   ├── HtmlSectionParser.kt       # parsing HTML de presentación reveal.js
    │   ├── AudioConversionUtil.kt    # conversión WAV→MP3
    │   ├── MediaProbeUtil.kt         # probing de duración ffprobe
    │   └── LanguageVoiceMapping.kt    # mapeo de voces FR/EN/ES/DE
    ├── test/kotlin/capsule/
    │   ├── CapsulePluginTest.kt
    │   ├── scenarios/                 # step definitions Cucumber BDD + runners
    │   │   ├── CapsuleStepDefinitions.kt
    │   │   ├── CucumberTestRunner.kt
    │   │   └── CucumberIntegrationTestRunner.kt
    │   └── *Test.kt                   # 17 clases de tests unitarios
    ├── test/features/
    │   └── capsule.feature            # 22 escenarios Cucumber
    └── functionalTest/kotlin/capsule/
        └── CapsulePluginFunctionalTest.kt
```

## Dependencia N2

`capsule-gradle` consume `slider-gradle` (`education.cccp:slider:0.0.6`) vía `compileOnly`.
El contrato se basa en archivos — capsule lee el HTML de la presentación y los archivos de texto
de script producidos por slider, nunca los modifica. CapsulePlugin aplica automáticamente
`education.cccp.slider` en runtime si está disponible.

## Bibliotecas clave

| Biblioteca | Versión | Rol |
|---------|---------|------|
| **Playwright** | 1.52.0 | Chromium headless — control reveal.js + captura de vídeo (JVM nativo, cero npm) |
| **koog-agents** | 0.8.0 | DSL Kotlin para orquestación de agentes (StateGraph / ConditionalEdges) |
| **Jackson** | 2.18.3 | Config YAML + serialización JSON (databind, kotlin, dataformat-yaml) |
| **Kover** | 0.9.8 | Informes de cobertura (XML + HTML, integrados en `check`) |
| **Cucumber** | 7.34.3 | Tests BDD (cucumber-java, junit-platform-engine, picocontainer) |
| **Kotlin** | 2.3.20 | DSL del plugin |
| **slider** | 0.0.6 | compileOnly — presentación reveal.js + script cápsula fuente |

Herramientas externas (no dependencias Maven):
- **Piper** — motor TTS local offline (por defecto)
- **espeak** — motor TTS de respaldo
- **FFmpeg** — recorte 9:16 (`deployCapsule`) y grabado de subtítulos
- **Manim** — renderizado de animación matemática (opcional, `[manim:SceneName]` en el script)
- **Chromium** — auto-descargado por Playwright en la primera ejecución

## Matriz de tests

| Tarea | Alcance | Detalles |
|------|-------|---------|
| `test` | Tests unitarios JUnit5 | Excluye la etiqueta `@integration` y `capsule.scenarios.**` |
| `cucumberTest` | BDD Cucumber | 22 escenarios, `forkEvery = 1`, `maxParallelForks = 1`, heap 1g |
| `functionalTest` | Gradle TestKit | Tests funcionales de aplicación del plugin |
| `check` | Todos | Depende de `test` + `cucumberTest` + `functionalTest` |

Etiquetas Cucumber: `@integration`, `@manim`, `@config`, `@tts`, `@subtitles`, `@burnin`, `@style`, `@parallel`。

Total de tests: **195 unitarios + 22 Cucumber = 217 PASS** (baseline sesión 029)。

## Ajuste JVM

Los tests Cucumber usan `maxHeapSize = "1g"` con `forkEvery = 1` (JVM nueva por escenario)
y `maxParallelForks = 1` (ejecución secuencial por estabilidad).

Para builds locales:
```bash
export GRADLE_OPTS="-Xmx2g"
```

## Comandos de build

```bash
./gradlew build                       # build completo (compila + testea)
./gradlew build -x test               # solo compilar
./gradlew check                       # todos los tests (unitarios + Cucumber + funcionales)
./gradlew test                        # solo JUnit5 unitarios
./gradlew cucumberTest                # BDD Cucumber
./gradlew functionalTest              # Gradle TestKit
./gradlew koverReport                 # informes de cobertura (XML + HTML)
./gradlew publishToMavenLocal         # publicación local
```

## Pipeline CI

`.github/workflows/test.yml` define un job:
1. **Build & Test** — `./gradlew check` en cada push/PR a main/master (≤ 15 min, JDK 24 Temurin, action setup Gradle)

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

## Publicación (NMCP)

**No publicado** — versión `0.0.1-SNAPSHOT`. El POM está configurado para Maven Central vía
los plugins `maven-publish` y `signing`:

- POM: nombre "Capsule Gradle Plugin", descripción "Generation automatisee de capsules video
  pedagogiques depuis des decks reveal.js", URL `github.com/cheroliv/capsule-gradle/`
- Firma: `useGpgCmd()` (configurado pero aún no ejecutado)
- Objetivo de publicación: `publishToMavenLocal` disponible; NMCP `publishAggregationToCentralPortal`
  requiere credenciales en `~/.gradle/gradle.properties`

Para publicar cuando esté listo:
```bash
./gradlew publishAggregationToCentralPortal --no-daemon
```

## Documentos de arquitectura

- [CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) — Arquitectura
  del pipeline con diagramas PlantUML (pipeline global, componentes, secuencia, despliegue)
- [.agents/INDEX.adoc](../capsule-plugin/.agents/INDEX.adoc) — EPICs, roadmap y gobernanza
- [AGENT.adoc](../capsule-plugin/AGENT.adoc) — Reglas absolutas (7 reglas, Ley de la Economía de Tinta)
- [CODE_REVIEW.adoc](../capsule-plugin/CODE_REVIEW.adoc) — Code review (10 EPICs accionables CR-1→CR-10)

## Estado de los EPICs

CAP-0 a CAP-25 terminados. EPICs activos (ver `.agents/INDEX.adoc`):

| EPIC | Descripción | Estado |
|------|-------------|--------|
| CR-1 | Refactorizar `CapsuleVideoTask.execute()` (218→76 líneas) | ✅ Terminado (sesión 040) |
| CR-2 | Thread safety + timeout `captureSlideParallel` | ⬜ TODO |
| CR-3 | Manejo de errores TTS — degradación señalizada | ⬜ TODO |
| CR-4 | Seguridad HTML escape — `injectSubtitleTrack` | ⬜ TODO |
| CR-5 | DRY `CapsuleConfigMerger` (~76 líneas duplicadas) | ⬜ TODO |
| CAP-27 | VTT burn-in (actualmente solo SRT) | ⬜ TODO |
| CAP-28 | Gaps de cobertura — PiperTtsEngine 31%, ManimEngineImpl 25% | ⬜ TODO |

## Contribuir

1. El build compila: `./gradlew build -x test`
2. Todos los tests en verde: `./gradlew check`
3. Seguir las 7 reglas absolutas (ver [AGENT.adoc](../capsule-plugin/AGENT.adoc)):
   - No commits/push sin permiso
   - DAG N2 — importable por N3, nunca importa N3
   - Capsule consume slider (solo lectura, nunca modifica la presentación fuente)
   - Cero secretos/tokens en el código
4. Respetar la Ley de la Economía de Tinta — nunca re-ejecutar operaciones costosas (TTS, captura Playwright)
   cuando ya existe un artefacto válido para la misma entrada

## Licencia

Apache License 2.0 — consulte [LICENSE](../LICENSE)。

---

_Parte del ecosistema CCCP Education — `groupId: education.cccp`._