<!-- translated from README.md rev 0.0.1-SNAPSHOT -->
# capsule-gradle — Guia do Consumidor

> Plugin do Gradle para geração automatizada de cápsulas de vídeo pedagógicas a partir de apresentações reveal.js.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.1-SNAPSHOT&color=orange)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=License)](../LICENSE)

- **Versão**: `0.0.1-SNAPSHOT` (não publicado) · **Grupo**: `education.cccp` · **ID do plugin**: `education.cccp.capsule`
- **Build**: `./gradlew build` · **Testes**: `./gradlew check` (195 unitários + 22 Cucumber PASS)
- **Cobertura**: relatórios Kover XML + HTML integrados em `check`

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | **Português** | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## O que faz

`capsule-gradle` transforma uma apresentação reveal.js (produzida por `slider-gradle`) numa
cápsula de vídeo pedagógica com narração TTS sincronizada — sem pós-produção.

O pipeline orquestra quatro ferramentas externas:

```
AsciiDoc source → slider-gradle → reveal.js deck + capsule script
                                      ↓
              capsule-gradle → TTS (Piper/espeak) → audio injection → Playwright capture → WebM video
                                      ↓                              ↓
                              Manim animation (optional)      FFmpeg distrib (9:16)
```

Parte do ecossistema multi-plugin CCCP Education (`groupId: education.cccp`).

## Início rápido

### 1. Aplicar o plugin

```gradle
plugins {
    id("education.cccp.capsule") version "0.0.1-SNAPSHOT"
}
```

### 2. Configurar a cápsula

```gradle
capsule {
    outputDir.set("capsules")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    ttsEngine.set("espeak")
}
```

### 3. Gerar o vídeo

```bash
./gradlew generateCapsuleVideo
```

Isto executa o pipeline completo: validação do script → síntese TTS → injeção de áudio
no HTML da apresentação → captura Playwright headless → saída WebM.

### Pipeline completo com slider

O `build.gradle.kts` raiz liga slider e capsule juntos:

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
./gradlew capsulevideo    # apresentação slider + script → vídeo cápsula
```

## Tarefas disponíveis

| Tarefa | Grupo | Descrição |
|------|-------|-------------|
| `generateCapsuleScript`   | generate  | Lê `*-script.txt` do slider e valida o script da cápsula |
| `generateCapsule`          | generate  | Gera ficheiros de áudio TTS a partir de scripts de cápsula (Piper/espeak) |
| `generateCapsuleVideo`     | generate  | Injeta o áudio TTS no HTML da apresentação e depois captura o vídeo via Playwright |
| `deployCapsule`            | deploy    | Recorta as cápsulas para vertical 9:16 (TikTok/Shorts) via FFmpeg |
| `collectCapsuleContext`    | collect   | Exporta o contexto das cápsulas (caminhos de vídeo + metadados) como JSON para o motor N3 |
| `transformCapsuleContext`  | transform | Analisa `capsule-context.json` e devolve uma lista de apresentações |
| `collectCapsuleRetrieve`   | collect   | Recupera apresentações de cápsula do JSON de contexto (contrato do motor N3) |
| `scaffoldCapsuleContext`   | generate  | Gera um ficheiro de configuração `capsule-context.yml` padrão com comentários |

## DSL de extensão

```gradle
capsule {
    // Entrada
    outputDir.set("capsule")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    chromiumExecutablePath.set("")

    // TTS
    ttsEngine.set("piper")                    // "piper" ou "espeak"
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

    // Legendas
    subtitleEnabled.set(false)
    subtitleFormat.set("srt")                 // "srt" ou "vtt"
    subtitleBurnIn.set(false)
    subtitleBurnInFontSize.set(24)
    subtitleBurnInFontColor.set("&H00FFFFFF")
    subtitleBurnInOutlineColor.set("&H00000000")
    subtitleBurnInPosition.set("bottom")      // "bottom" ou "top"

    // Distribuição
    ffmpegExecutablePath.set("ffmpeg")
    distribOutputWidth.set(1080)
    distribOutputHeight.set(1920)

    // Animação Manim
    manimExecutablePath.set("manim")
    manimQuality.set("l")                     // l, m, h, p, k
    manimScriptsDir.set("src/manim")
    manimOutputDir.set("build/capsule/manim")
    manimParallelRender.set(false)
    manimParallelRenderThreads.set(4)
}
```

### Configuração multi-fonte

A configuração é resolvida a partir de 4 fontes (prioridade: CLI > YAML > gradle.properties > ENV):

```bash
./gradlew scaffoldCapsuleContext          # gerar capsule-context.yml
./gradlew generateCapsuleVideo -Pcapsule.tts.engine=espeak -Pcapsule.manim.quality=h
```

## Pré-requisitos

- **Java** 24+ (toolchain Kotlin 2.3.20)
- **Gradle** 9.5+
- **Piper** (TTS, local offline) — motor padrão
- **espeak** (TTS de recurso)
- **Chromium** (auto-descarregado pelo Playwright na primeira execução)
- **FFmpeg** (para `deployCapsule` recorte 9:16 e burn-in de legendas)
- **Manim** (opcional, para diapositivos de animação matemática)

## Build e testes

```bash
./gradlew build                    # build completo
./gradlew check                    # testes unitários + funcionais + Cucumber
./gradlew test                     # testes unitários JUnit5 (exclui @integration)
./gradlew cucumberTest             # testes BDD Cucumber
./gradlew functionalTest           # testes funcionais Gradle TestKit
./gradlew publishToMavenLocal      # publicar localmente
```

## Resolução de problemas

| Sintoma | Solução |
|---------|-----|
| Playwright Chromium não encontrado     | executar uma vez para auto-descarregar, ou definir `chromiumExecutablePath` |
| Piper TTS não produz áudio       | verificar `piperExecutablePath`, garantir que o modelo de voz está instalado |
| `deployCapsule` falha              | verificar que FFmpeg está no PATH (`ffmpeg -version`) |
| Diapositivos Manim não renderizados          | definir `manimExecutablePath` como `noop` para testar sem Manim |
| Timeout na captura de vídeo              | aumentar `playwrightTimeout` (padrão 120000ms) |
| Sem memória                      | `export GRADLE_OPTS="-Xmx2g"` |

Consulte [CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) para detalhes do pipeline.

## Licença

Apache License 2.0 — consulte [LICENSE](../LICENSE).

---

_Parte do ecossistema CCCP Education — `groupId: education.cccp`._