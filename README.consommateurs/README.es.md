<!-- translated from README.md rev 0.0.10 -->
# capsule-gradle — Guía del consumidor

> Plugin Gradle que convierte un deck reveal.js en una cápsula de vídeo sincronizada — locución TTS, captura, subtítulos, conversión de formato, audio post y un pipeline corto/viral — sin postproducción humana.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.10&color=green)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![Licencia](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=Licencia)](../LICENSE)

- **Versión**: `0.0.10` (Maven Central) · **Grupo**: `education.cccp` · **ID del plugin**: `education.cccp.capsule`
- **Requiere**: Java 25+ · Gradle 9.8+
- **Docs**: [interioridades del plugin](../capsule-plugin/README.md) · [arquitectura del pipeline](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc)

🌐 Idiomas: [English](../README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | **Español** | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Qué hace

`capsule-gradle` consume un deck reveal.js (producido por `slider-gradle`) y produce una
cápsula de vídeo sincronizada — una locución TTS por diapositiva, captura de vídeo,
subtítulos, conversión de formato, postproducción de audio, control de calidad y un
pipeline vertical corto/viral opcional.

```
Fuente AsciiDoc → slider-gradle → deck reveal.js + script del orador
                                        ↓
        capsule-gradle → TTS (Piper/espeak) → inyección de audio → captura → WebM/MP4
                                        ↓                            ↓
                              animación Manim (opcional)  recorte 9:16 con FFmpeg (opcional)
```

Parte del ecosistema multi-plugin CCCP Education (`groupId: education.cccp`).

## Requisitos

- **Java** 25+ (toolchain Kotlin 2.4.10)
- **Gradle** 9.8+
- **Piper** — TTS local sin conexión (motor por defecto)
- **espeak / espeak-ng** — TTS de reserva (`espeak-ng` se usa automáticamente si falta `espeak`)
- **Fish Audio** — TTS en la nube opcional con **voz clonada** (motor `fish`); el pool de claves vive en un `fish.yaml` ignorado por git
- **Chromium** — descargado automáticamente por Playwright en la primera ejecución
- **FFmpeg** — recorte 9:16, burn-in de subtítulos, conversión a MP4, audio post
- **Manim** — opcional, para diapositivas matemáticas animadas
- **Node** — opcional, para la estrategia de captura/animación Remotion

## Inicio rápido

### 1. Aplicar el plugin

```gradle
plugins {
    id("education.cccp.capsule") version "0.0.10"
}
```

### 2. Configurar

```gradle
capsule {
    outputDir.set("capsules")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    ttsEngine.set("espeak")   // "piper" (por defecto), "espeak" o "fish" (Fish Audio nube, voz clonada)
}
```

### 3. Generar el vídeo

```bash
./gradlew generateCapsuleVideo
```

Esto ejecuta todo el pipeline: validación del script → síntesis TTS → inyección de audio en
el HTML del deck → captura headless → salida de vídeo.

### Pipeline completo con slider

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
./gradlew generateCapsuleVideo    # deck + script de slider → cápsula de vídeo
```

## Tareas disponibles

| Tarea | Grupo | Descripción |
|-------|-------|-------------|
| `generateCapsuleScript` | generate | Lee el `*-script.txt` producido por slider y valida el script de la cápsula |
| `generateCapsule` | generate | Sintetiza los archivos de audio TTS (Piper/espeak), uno por diapositiva |
| `generateCapsuleVideo` | generate | Inyecta el audio TTS en el HTML del deck y luego captura el vídeo |
| `generateCapsuleVideoAllLanguages` | generate | Una cápsula localizada por idioma destino (`LanguageCatalog`) |
| `extractSpeakerNotes` | capsule | Analiza las notas del orador AsciiDoc al contrato `*-script.txt` de texto plano |
| `translateAndExtractSpeakerNotes` | capsule | Compuesto: `translateDeck` (slider) → `extractSpeakerNotes` |
| `translateAndGenerateCapsuleVideos` | capsule | Compuesto: `translateDeck` → `extractSpeakerNotes` → `generateCapsuleVideoAllLanguages` |
| `generateCapsuleContent` | generate | Pipeline LLM (koog): propose-context → validación → notas del orador + script TTS |
| `generateCapsuleContentAndVideos` | capsule | Compuesto: `generateCapsuleContent` → `translateAndGenerateCapsuleVideos` |
| `collectCapsuleAugmentedContext` | collect | Recoge el contexto aumentado (gobernanza + RAG + Docs + escenario + glosario) |
| `capsuleAiSmokeTest` | generate | Prueba de humo del puente LLM de codebase con un prompt mínimo |
| `generateCapsuleTranscript` | generate | Produce un artículo de transcripción AsciiDoc a partir de las notas del orador |
| `generateCapsuleChapters` | generate | Marcadores de capítulo + capítulos de texto de YouTube + descripción |
| `generateCapsulePodcast` | generate | Podcast solo de audio (narración MP3 concatenada) |
| `deployCapsule` | deploy | Recorta la cápsula a vertical 9:16 (TikTok/Shorts) con FFmpeg |
| `distributeCapsuleVideo` | distribute | Copia los vídeos finales a un directorio de destino versionado |
| `collectCapsuleContext` / `transformCapsuleContext` / `collectCapsuleRetrieve` | collect/transform | Exporta / analiza el JSON de contexto de la cápsula (contrato del motor N3) |
| `scaffoldCapsuleContext` | generate | Genera un `capsule-context.yml` por defecto comentado |
| `validateCapsuleVideoDuration` | verification | Sondea la duración del vídeo frente a la suma de las duraciones de audio TTS (opt-in) |
| `preflightCapsuleContent` | verification | Pre-vuelo del contenido antes del TTS facturado (opt-in) |
| `renderQcBoards` | verification | Renderiza las planchas QC (beats + «dicho vs mostrado») (opt-in) |
| `verifyCapsuleVoice` | verification | QC de voz por ASR — fidelidad, respiración, elección de toma (opt-in) |
| `selectBestVoiceTake` | verification | Selecciona la mejor toma entre las candidatas de una diapositiva (opt-in) |
| `generateViralCampaign` | generate | Storyboard → campaña corta (YouTube/TikTok) (opt-in) |
| `renderViralVariants` / `renderViralThumbnails` / `renderViralSubtitles` | generate | Renderiza las variantes virales, miniaturas, subtítulos karaoke (opt-in) |
| `validateViralCampaign` | verification | Comprueba el manifiesto frente a los archivos producidos (opt-in) |
| `normalizeViralStoryboard` | generate | Completa las transiciones faltantes del storyboard (ayuda de autoría) |

> Las capacidades opcionales (*opt-in*) están **desactivadas por defecto** (`false`) y nunca
> bloquean el pipeline principal.

## Configuración

Cada valor se resuelve desde **4 fuentes** (prioridad **CLI > YAML > gradle.properties > ENV**):

```bash
./gradlew scaffoldCapsuleContext          # genera un capsule-context.yml comentado
./gradlew generateCapsuleVideo -Pcapsule.tts.engine=espeak -Pcapsule.manim.quality=h
```

DSL representativo (ver la [guía de interioridades](../capsule-plugin/README.md) para el
`CapsuleConfig` completo de 18 secciones):

```gradle
capsule {
    // Entrada
    outputDir.set("capsule")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")

    // TTS
    ttsEngine.set("piper")                 // "piper" (por defecto) o "espeak"
    ttsVoice.set("fr_FR-siwis-medium")
    piperExecutablePath.set("piper")
    ttsFallbackEnabled.set(true)
    espeakVoice.set("fr")
    espeakSpeed.set(150)
    ttsLanguage.set("fr")                  // 22 idiomas (LanguageCatalog)

    // Captura / salida
    viewportWidth.set(1408)
    viewportHeight.set(792)
    captureStrategy.set("playwright")      // "playwright" | "screenshot" | "remotion"
    outputFormat.set("webm")               // "webm" (por defecto) | "mp4" | "both"
    outputBt709.set(false)                 // etiqueta el MP4 como BT.709 (opt-in)

    // Subtítulos
    subtitleEnabled.set(false)
    subtitleFormat.set("srt")              // "srt" o "vtt"
    subtitleBurnIn.set(false)

    // Distribución
    ffmpegExecutablePath.set("ffmpeg")
    distribOutputWidth.set(1080)
    distribOutputHeight.set(1920)

    // Manim (opcional)
    manimExecutablePath.set("manim")
    manimQuality.set("l")                  // l, m, h, p, k
}
```

## Pipeline multi-idioma

Un deck fuente → una cápsula localizada por idioma destino. El pipeline consume los decks
traducidos producidos por `slider.translateDeck` (**22 idiomas**, `LanguageCatalog` del
contrato N0 `i18n-contracts`) y resuelve la voz TTS por idioma vía el dominio puro
`capsule.multilang` (`MultiLanguageResolver` + `VoiceMapping`).

```
deck fuente (AsciiDoc)
        ↓ slider.translateDeck (LanguageCatalog, 22 idiomas)
<deckName>_<lang>-deck.html (uno por idioma)
        ↓ capsule.extractSpeakerNotes
<deckName>_<lang>-script.txt (uno por idioma)
        ↓ capsule.generateCapsuleVideoAllLanguages
<deckName>_<lang>.webm (una cápsula localizada por idioma)
```

Convención de nombres (contrato de frontera, alineado con slider):

- deck traducido: `<deckName>_<lang>-deck.html`
- script del orador: `<deckName>_<lang>-script.txt`
- vídeo localizado: `<deckName>_<lang>.webm`

```bash
./gradlew generateCapsuleVideoAllLanguages   # un vídeo por idioma con par deck+script
./gradlew translateAndGenerateCapsuleVideos  # compuesto completo: translate → extract → vídeos
```

Un idioma sin par deck + script correspondiente se omite en silencio, y se aplica la regla de
**Economía de Tinta**: un idioma cuyo vídeo ya existe y sondea válido nunca se re-renderiza.

## Funcionalidades opcionales

- **Formato de salida** — `outputFormat = "mp4"` (o `"both"`) produce un MP4/H.264 además del
  WebM nativo; `outputBt709 = true` lo etiqueta como BT.709 (colorimetría de difusión).
- **Postproducción de audio** — música de fondo, loudness EBU R128 (medición en 2 pasadas) y
  ducking sidechain (`audioPost { … }`).
- **Subtítulos** — generación SRT/VTT y burn-in (`subtitleEnabled`, `subtitleBurnIn`).
- **Control de calidad (opt-in)** — pre-vuelo del contenido (`preflightCapsuleContent`),
  integridad de vídeo por decodificación, timeline determinista, planchas QC
  (`renderQcBoards`) y QC de voz por ASR (`verifyCapsuleVoice`, `selectBestVoiceTake`).
- **Transcript** — pipeline inverso: un artículo AsciiDoc a partir de las notas del orador
  (`TEMPLATE` o `LLM`).
- **Podcast** — MP3 solo de audio de la narración.
- **Preview** — dry-run (`previewOnly = true`): renderiza solo los PNG de diapositivas, omite el mux.
- **Viral (opt-in)** — un storyboard validable → campaña corta vertical 9:16 con hooks/CTA
  anclados en el contexto aumentado, miniaturas, subtítulos karaoke y un `CampaignBundle`.

### Ritmo de las diapositivas (captura Remotion)

Con `captureStrategy = "remotion"`, el deck se anima en lugar de filmarse: cada bloque de una
diapositiva aparece con un ritmo de lectura en vez de que toda la diapositiva aparezca de
golpe. Beats por defecto (segundos desde el inicio de la diapositiva): `.kicker` 0.0 ·
`h1`/`h2` 0.15 · `.rule` 0.5 · `.lede` 1.0 · `.stat` 4.5 · `.statlabel` 5.0. `.brand`, `.num`
y `.grid` son chrome (presentes desde el primer fotograma). Cualquier bloque puede fijar su
beat con `data-at`:

```html
<p class="stat" data-at="8.5">542 076 km</p>
```

## Build y test (contribuidores)

```bash
./gradlew build                    # build completo
./gradlew check                    # unitarias + funcionales + Cucumber
./gradlew test                     # solo unitarias JUnit5
./gradlew cucumberTest -PrunCucumber
./gradlew functionalTest           # Gradle TestKit
./gradlew publishToMavenLocal      # publicación local
```

## Solución de problemas

| Síntoma | Solución |
|---------|----------|
| Chromium de Playwright no encontrado | ejecute una vez para descargarlo, o defina `chromiumExecutablePath` |
| Piper TTS no produce audio | compruebe `piperExecutablePath`, asegúrese de que el modelo de voz esté instalado |
| `deployCapsule` falla | verifique que FFmpeg esté en el `PATH` (`ffmpeg -version`) |
| Diapositivas Manim no renderizadas | ponga `manimExecutablePath` en `noop` para probar sin Manim |
| Timeout de captura de vídeo | aumente `playwrightTimeout` (por defecto 120000 ms) |
| Memoria insuficiente | `export GRADLE_OPTS="-Xmx2g"` |

## Documentación

- [capsule-plugin/README.md](../capsule-plugin/README.md) — interioridades del plugin (guía de desarrollador y contribuidor)
- [capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) — arquitectura del pipeline (PlantUML)

## Licencia

Apache License 2.0 — ver [LICENSE](../LICENSE).

---

_Parte del ecosistema CCCP Education — `groupId: education.cccp`._
