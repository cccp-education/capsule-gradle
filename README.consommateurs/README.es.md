<!-- translated from README.md rev 0.0.1-SNAPSHOT -->
# capsule-gradle — Guía del Consumidor

> Plugin de Gradle para la generación automatizada de cápsulas de vídeo pedagógicas a partir de presentaciones reveal.js.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.1-SNAPSHOT&color=orange)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=License)](../LICENSE)

- **Versión**: `0.0.1-SNAPSHOT` (no publicado) · **Grupo**: `education.cccp` · **ID del plugin**: `education.cccp.capsule`
- **Build**: `./gradlew build` · **Tests**: `./gradlew check` (195 unitarios + 22 Cucumber PASS)
- **Cobertura**: informes Kover XML + HTML integrados en `check`

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | **Español** | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Qué hace

`capsule-gradle` transforma una presentación reveal.js (producida por `slider-gradle`) en una
cápsula de vídeo pedagógica con narración TTS sincronizada — sin postproducción.

El pipeline orquesta cuatro herramientas externas:

```
AsciiDoc source → slider-gradle → reveal.js deck + capsule script
                                      ↓
              capsule-gradle → TTS (Piper/espeak) → audio injection → Playwright capture → WebM video
                                      ↓                              ↓
                              Manim animation (optional)      FFmpeg distrib (9:16)
```

Parte del ecosistema multi-plugin de CCCP Education (`groupId: education.cccp`).

## Inicio rápido

### 1. Aplicar el plugin

```gradle
plugins {
    id("education.cccp.capsule") version "0.0.1-SNAPSHOT"
}
```

### 2. Configurar la cápsula

```gradle
capsule {
    outputDir.set("capsules")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    ttsEngine.set("espeak")
}
```

### 3. Generar el vídeo

```bash
./gradlew generateCapsuleVideo
```

Esto ejecuta el pipeline completo: validación del script → síntesis TTS → inyección de audio
en el HTML de la presentación → captura Playwright headless → salida WebM.

### Pipeline completo con slider

El `build.gradle.kts` raíz conecta slider y capsule juntos:

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
./gradlew capsulevideo    # presentación slider + script → vídeo cápsula
```

## Tareas disponibles

| Tarea | Grupo | Descripción |
|------|-------|-------------|
| `generateCapsuleScript`   | generate  | Lee `*-script.txt` de slider y valida el script de la cápsula |
| `generateCapsule`          | generate  | Genera archivos de audio TTS a partir de scripts de cápsula (Piper/espeak) |
| `generateCapsuleVideo`     | generate  | Inyecta el audio TTS en el HTML de la presentación y luego captura el vídeo vía Playwright |
| `deployCapsule`            | deploy    | Recorta las cápsulas a formato vertical 9:16 (TikTok/Shorts) vía FFmpeg |
| `collectCapsuleContext`    | collect   | Exporta el contexto de las cápsulas (rutas de vídeo + metadatos) como JSON para el motor N3 |
| `transformCapsuleContext`  | transform | Analiza `capsule-context.json` y devuelve una lista de presentaciones |
| `collectCapsuleRetrieve`   | collect   | Recupera las presentaciones de cápsula desde el JSON de contexto (contrato del motor N3) |
| `scaffoldCapsuleContext`   | generate  | Genera un archivo de configuración `capsule-context.yml` por defecto con comentarios |

## DSL de extensión

```gradle
capsule {
    // Entrada
    outputDir.set("capsule")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    chromiumExecutablePath.set("")

    // TTS
    ttsEngine.set("piper")                    // "piper" o "espeak"
    ttsVoice.set("fr_FR-siwis-medium")
    piperExecutablePath.set("piper")
    ttsFallbackEnabled.set(true)
    espeakVoice.set("fr")
    espeakSpeed.set(150)
    ttsLanguage.set("fr")                     // fr, en, es, de

    // Captura
    viewportWidth.set(1408)
    viewportHeight.set(792)
    playwrightTimeout.set(120_000.0)
    slideDurationSeconds.set(5.0)
    parallelCaptureEnabled.set(false)
    parallelCaptureThreads.set(4)

    // Subtítulos
    subtitleEnabled.set(false)
    subtitleFormat.set("srt")                 // "srt" o "vtt"
    subtitleBurnIn.set(false)
    subtitleBurnInFontSize.set(24)
    subtitleBurnInFontColor.set("&H00FFFFFF")
    subtitleBurnInOutlineColor.set("&H00000000")
    subtitleBurnInPosition.set("bottom")      // "bottom" o "top"

    // Distribución
    ffmpegExecutablePath.set("ffmpeg")
    distribOutputWidth.set(1080)
    distribOutputHeight.set(1920)

    // Animación Manim
    manimExecutablePath.set("manim")
    manimQuality.set("l")                     // l, m, h, p, k
    manimScriptsDir.set("src/manim")
    manimOutputDir.set("build/capsule/manim")
    manimParallelRender.set(false)
    manimParallelRenderThreads.set(4)
}
```

### Configuración multi-fuente

La configuración se resuelve desde 4 fuentes (prioridad: CLI > YAML > gradle.properties > ENV):

```bash
./gradlew scaffoldCapsuleContext          # generar capsule-context.yml
./gradlew generateCapsuleVideo -Pcapsule.tts.engine=espeak -Pcapsule.manim.quality=h
```

## Requisitos previos

- **Java** 24+ (toolchain Kotlin 2.3.20)
- **Gradle** 9.5+
- **Piper** (TTS, local offline) — motor por defecto
- **espeak** (TTS de respaldo)
- **Chromium** (auto-descargado por Playwright en la primera ejecución)
- **FFmpeg** (para `deployCapsule` recorte 9:16 y grabado de subtítulos)
- **Manim** (opcional, para diapositivas de animación matemática)

## Build y tests

```bash
./gradlew build                    # build completo
./gradlew check                    # tests unitarios + funcionales + Cucumber
./gradlew test                     # tests unitarios JUnit5 (excluye @integration)
./gradlew cucumberTest             # tests BDD Cucumber
./gradlew functionalTest           # tests funcionales Gradle TestKit
./gradlew publishToMavenLocal      # publicar localmente
```

## Solución de problemas

| Síntoma | Solución |
|---------|-----|
| Playwright Chromium no encontrado     | ejecutar una vez para auto-descargar, o definir `chromiumExecutablePath` |
| Piper TTS no produce audio       | comprobar `piperExecutablePath`, asegurar que el modelo de voz está instalado |
| `deployCapsule` falla              | verificar que FFmpeg está en el PATH (`ffmpeg -version`) |
| Diapositivas Manim no renderizadas          | definir `manimExecutablePath` como `noop` para probar sin Manim |
| Timeout en la captura de vídeo              | aumentar `playwrightTimeout` (por defecto 120000ms) |
| Sin memoria                      | `export GRADLE_OPTS="-Xmx2g"` |

Consulte [CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) para detalles del pipeline.

## Licencia

Apache License 2.0 — consulte [LICENSE](../LICENSE).

---

_Parte del ecosistema CCCP Education — `groupId: education.cccp`._