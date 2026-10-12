<!-- translated from README.md rev 0.0.8 -->
# capsule-gradle — Guia do consumidor

> Plugin Gradle que transforma um deck reveal.js numa cápsula de vídeo sincronizada — narração TTS, captura, legendas, conversão de formato, áudio pós e um pipeline curto/viral — sem pós-produção humana.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.8&color=green)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![Licença](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=Licen%C3%A7a)](../LICENSE)

- **Versão**: `0.0.8` (Maven Central) · **Grupo**: `education.cccp` · **ID do plugin**: `education.cccp.capsule`
- **Requer**: Java 25+ · Gradle 9.8+
- **Docs**: [interiores do plugin](../capsule-plugin/README.md) · [arquitetura do pipeline](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc)

🌐 Idiomas: [English](../README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | **Português** | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## O que faz

`capsule-gradle` consome um deck reveal.js (produzido pelo `slider-gradle`) e produz uma
cápsula de vídeo sincronizada — uma narração TTS por slide, captura de vídeo, legendas,
conversão de formato, pós-produção de áudio, controle de qualidade e um pipeline vertical
curto/viral opcional.

```
Fonte AsciiDoc → slider-gradle → deck reveal.js + script do orador
                                        ↓
        capsule-gradle → TTS (Piper/espeak) → injeção de áudio → captura → WebM/MP4
                                        ↓                           ↓
                              animação Manim (opcional)   recorte 9:16 com FFmpeg (opcional)
```

Parte do ecossistema multi-plugin CCCP Education (`groupId: education.cccp`).

## Requisitos

- **Java** 25+ (toolchain Kotlin 2.4.10)
- **Gradle** 9.8+
- **Piper** — TTS local offline (motor padrão)
- **espeak / espeak-ng** — TTS de fallback (`espeak-ng` é usado automaticamente se `espeak` estiver ausente)
- **Chromium** — baixado automaticamente pelo Playwright na primeira execução
- **FFmpeg** — recorte 9:16, burn-in de legendas, conversão MP4, áudio pós
- **Manim** — opcional, para slides matemáticos animados
- **Node** — opcional, para a estratégia de captura/animação Remotion

## Início rápido

### 1. Aplicar o plugin

```gradle
plugins {
    id("education.cccp.capsule") version "0.0.8"
}
```

### 2. Configurar

```gradle
capsule {
    outputDir.set("capsules")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    ttsEngine.set("espeak")   // "piper" (padrão) ou "espeak"
}
```

### 3. Gerar o vídeo

```bash
./gradlew generateCapsuleVideo
```

Isso executa todo o pipeline: validação do script → síntese TTS → injeção de áudio no HTML
do deck → captura headless → saída de vídeo.

### Pipeline completo com slider

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
./gradlew generateCapsuleVideo    # deck + script do slider → cápsula de vídeo
```

## Tarefas disponíveis

| Tarefa | Grupo | Descrição |
|--------|-------|-----------|
| `generateCapsuleScript` | generate | Lê o `*-script.txt` produzido pelo slider e valida o script da cápsula |
| `generateCapsule` | generate | Sintetiza os arquivos de áudio TTS (Piper/espeak), um por slide |
| `generateCapsuleVideo` | generate | Injeta o áudio TTS no HTML do deck e então captura o vídeo |
| `generateCapsuleVideoAllLanguages` | generate | Uma cápsula localizada por idioma-alvo (`LanguageCatalog`) |
| `extractSpeakerNotes` | capsule | Analisa as notas do orador do AsciiDoc no contrato `*-script.txt` de texto |
| `translateAndExtractSpeakerNotes` | capsule | Composto: `translateDeck` (slider) → `extractSpeakerNotes` |
| `translateAndGenerateCapsuleVideos` | capsule | Composto: `translateDeck` → `extractSpeakerNotes` → `generateCapsuleVideoAllLanguages` |
| `generateCapsuleContent` | generate | Pipeline LLM (koog): propose-context → validação → notas do orador + script TTS |
| `generateCapsuleContentAndVideos` | capsule | Composto: `generateCapsuleContent` → `translateAndGenerateCapsuleVideos` |
| `collectCapsuleAugmentedContext` | collect | Coleta o contexto aumentado (governança + RAG + Docs + cenário + glossário) |
| `capsuleAiSmokeTest` | generate | Teste de fumaça da ponte LLM do codebase com um prompt mínimo |
| `generateCapsuleTranscript` | generate | Produz um artigo de transcrição AsciiDoc a partir das notas do orador |
| `generateCapsuleChapters` | generate | Marcadores de capítulo + capítulos de texto do YouTube + descrição |
| `generateCapsulePodcast` | generate | Podcast só de áudio (narração MP3 concatenada) |
| `deployCapsule` | deploy | Recorta a cápsula para vertical 9:16 (TikTok/Shorts) via FFmpeg |
| `distributeCapsuleVideo` | distribute | Copia os vídeos finais para um diretório de destino versionado |
| `collectCapsuleContext` / `transformCapsuleContext` / `collectCapsuleRetrieve` | collect/transform | Exporta / analisa o JSON de contexto da cápsula (contrato do motor N3) |
| `scaffoldCapsuleContext` | generate | Gera um `capsule-context.yml` padrão comentado |
| `validateCapsuleVideoDuration` | verification | Sonda a duração do vídeo face à soma das durações de áudio TTS (opt-in) |
| `preflightCapsuleContent` | verification | Pré-voo do conteúdo antes do TTS pago (opt-in) |
| `renderQcBoards` | verification | Renderiza as pranchas QC (beats + «dito vs mostrado») (opt-in) |
| `verifyCapsuleVoice` | verification | QC de voz por ASR — fidelidade, respiração, escolha de take (opt-in) |
| `selectBestVoiceTake` | verification | Seleciona o melhor take entre os candidatos de um slide (opt-in) |
| `generateViralCampaign` | generate | Storyboard → campanha curta (YouTube/TikTok) (opt-in) |
| `renderViralVariants` / `renderViralThumbnails` / `renderViralSubtitles` | generate | Renderiza as variantes virais, miniaturas, legendas karaokê (opt-in) |
| `validateViralCampaign` | verification | Verifica o manifesto face aos arquivos produzidos (opt-in) |
| `normalizeViralStoryboard` | generate | Preenche as transições ausentes do storyboard (ajuda de autoria) |

> As capacidades opcionais (*opt-in*) estão **desativadas por padrão** (`false`) e nunca
> bloqueiam o pipeline principal.

## Configuração

Cada valor é resolvido a partir de **4 fontes** (precedência **CLI > YAML > gradle.properties > ENV**):

```bash
./gradlew scaffoldCapsuleContext          # gera um capsule-context.yml comentado
./gradlew generateCapsuleVideo -Pcapsule.tts.engine=espeak -Pcapsule.manim.quality=h
```

DSL representativo (ver o [guia de interiores](../capsule-plugin/README.md) para a
`CapsuleConfig` completa de 18 seções):

```gradle
capsule {
    // Entrada
    outputDir.set("capsule")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")

    // TTS
    ttsEngine.set("piper")                 // "piper" (padrão) ou "espeak"
    ttsVoice.set("fr_FR-siwis-medium")
    piperExecutablePath.set("piper")
    ttsFallbackEnabled.set(true)
    espeakVoice.set("fr")
    espeakSpeed.set(150)
    ttsLanguage.set("fr")                  // 22 idiomas (LanguageCatalog)

    // Captura / saída
    viewportWidth.set(1408)
    viewportHeight.set(792)
    captureStrategy.set("playwright")      // "playwright" | "screenshot" | "remotion"
    outputFormat.set("webm")               // "webm" (padrão) | "mp4" | "both"
    outputBt709.set(false)                 // marca o MP4 como BT.709 (opt-in)

    // Legendas
    subtitleEnabled.set(false)
    subtitleFormat.set("srt")              // "srt" ou "vtt"
    subtitleBurnIn.set(false)

    // Distribuição
    ffmpegExecutablePath.set("ffmpeg")
    distribOutputWidth.set(1080)
    distribOutputHeight.set(1920)

    // Manim (opcional)
    manimExecutablePath.set("manim")
    manimQuality.set("l")                  // l, m, h, p, k
}
```

## Pipeline multi-idioma

Um deck de origem → uma cápsula localizada por idioma-alvo. O pipeline consome os decks
traduzidos produzidos por `slider.translateDeck` (**22 idiomas**, `LanguageCatalog` do
contrato N0 `i18n-contracts`) e resolve a voz TTS por idioma via o domínio puro
`capsule.multilang` (`MultiLanguageResolver` + `VoiceMapping`).

```
deck de origem (AsciiDoc)
        ↓ slider.translateDeck (LanguageCatalog, 22 idiomas)
<deckName>_<lang>-deck.html (um por idioma)
        ↓ capsule.extractSpeakerNotes
<deckName>_<lang>-script.txt (um por idioma)
        ↓ capsule.generateCapsuleVideoAllLanguages
<deckName>_<lang>.webm (uma cápsula localizada por idioma)
```

Convenção de nomes (contrato de fronteira, alinhado ao slider):

- deck traduzido: `<deckName>_<lang>-deck.html`
- script do orador: `<deckName>_<lang>-script.txt`
- vídeo localizado: `<deckName>_<lang>.webm`

```bash
./gradlew generateCapsuleVideoAllLanguages   # um vídeo por idioma com par deck+script
./gradlew translateAndGenerateCapsuleVideos  # composto completo: translate → extract → vídeos
```

Um idioma sem par deck + script correspondente é ignorado silenciosamente, e aplica-se a
regra de **Economia de Tinta**: um idioma cujo vídeo já existe e sonda válido nunca é
re-renderizado.

## Funcionalidades opcionais

- **Formato de saída** — `outputFormat = "mp4"` (ou `"both"`) produz um MP4/H.264 além do WebM
  nativo; `outputBt709 = true` marca-o como BT.709 (colorimetria de difusão).
- **Pós-produção de áudio** — música de fundo, loudness EBU R128 (medição em 2 passagens) e
  ducking sidechain (`audioPost { … }`).
- **Legendas** — geração SRT/VTT e burn-in (`subtitleEnabled`, `subtitleBurnIn`).
- **Controle de qualidade (opt-in)** — pré-voo do conteúdo (`preflightCapsuleContent`),
  integridade de vídeo por decodificação, timeline determinística, pranchas QC
  (`renderQcBoards`) e QC de voz por ASR (`verifyCapsuleVoice`, `selectBestVoiceTake`).
- **Transcript** — pipeline inverso: um artigo AsciiDoc a partir das notas do orador
  (`TEMPLATE` ou `LLM`).
- **Podcast** — MP3 só de áudio da narração.
- **Preview** — dry-run (`previewOnly = true`): renderiza apenas os PNG dos slides, pula o mux.
- **Viral (opt-in)** — um storyboard validável → campanha curta vertical 9:16 com hooks/CTA
  ancorados no contexto aumentado, miniaturas, legendas karaokê e um `CampaignBundle`.

### Ritmo dos slides (captura Remotion)

Com `captureStrategy = "remotion"`, o deck é animado em vez de filmado: cada bloco de um
slide aparece num ritmo de leitura em vez de o slide inteiro aparecer de uma vez. Beats
padrão (segundos desde o início do slide): `.kicker` 0.0 · `h1`/`h2` 0.15 · `.rule` 0.5 ·
`.lede` 1.0 · `.stat` 4.5 · `.statlabel` 5.0. `.brand`, `.num` e `.grid` são chrome
(presentes desde o primeiro quadro). Qualquer bloco pode definir seu beat com `data-at`:

```html
<p class="stat" data-at="8.5">542 076 km</p>
```

## Build e teste (contribuidores)

```bash
./gradlew build                    # build completo
./gradlew check                    # unitários + funcionais + Cucumber
./gradlew test                     # só unitários JUnit5
./gradlew cucumberTest -PrunCucumber
./gradlew functionalTest           # Gradle TestKit
./gradlew publishToMavenLocal      # publicação local
```

## Solução de problemas

| Sintoma | Correção |
|---------|----------|
| Chromium do Playwright não encontrado | execute uma vez para baixá-lo, ou defina `chromiumExecutablePath` |
| Piper TTS não produz áudio | verifique `piperExecutablePath`, garanta que o modelo de voz esteja instalado |
| `deployCapsule` falha | verifique se o FFmpeg está no `PATH` (`ffmpeg -version`) |
| Slides Manim não renderizados | defina `manimExecutablePath` como `noop` para testar sem Manim |
| Timeout de captura de vídeo | aumente `playwrightTimeout` (padrão 120000 ms) |
| Memória insuficiente | `export GRADLE_OPTS="-Xmx2g"` |

## Documentação

- [capsule-plugin/README.md](../capsule-plugin/README.md) — interiores do plugin (guia de desenvolvedor e contribuidor)
- [capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) — arquitetura do pipeline (PlantUML)

## Licença

Apache License 2.0 — ver [LICENSE](../LICENSE).

---

_Parte do ecossistema CCCP Education — `groupId: education.cccp`._
