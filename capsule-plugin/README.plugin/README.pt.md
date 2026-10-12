<!-- translated from README.md rev 0.0.10 -->
# capsule-gradle — Interiores do Plugin

> Guia para desenvolvedores e contribuidores do plugin Gradle `capsule-plugin`.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.10&color=green)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![Cobertura](https://img.shields.io/static/v1?label=cobertura&message=Kover&color=blue)]()
[![Licença](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=Licen%C3%A7a)](../../LICENSE)

- **Versão**: `0.0.10` (Maven Central) · **Grupo**: `education.cccp` · **ID do plugin**: `education.cccp.capsule`
- **Toolchain**: Java 25 · Kotlin 2.4.10 · Gradle 9.8.1
- **Build**: `./gradlew build -x test` · **Testes**: `./gradlew check` (unitários + funcionais via TestKit; Cucumber com `-PrunCucumber`) · **Cobertura**: `./gradlew koverReport`

🌐 Idiomas: [English](../README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | **Português** | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## O que faz

`capsule-gradle` transforma um deck reveal.js (produzido pelo `slider-gradle`) numa
cápsula de vídeo sincronizada — narração TTS por slide, captura de vídeo, legendas,
conversão de formato, pós-produção de áudio, controle de qualidade e um pipeline
vertical curto/viral — sem pós-produção humana. É um plugin Gradle N2
(consumido pelo runner N3, nunca é o próprio N3).

## Estrutura do módulo

```
capsule-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml            # catálogo de versões interno
├── doc/CAPSULE_ARCHITECTURE.adoc        # arquitetura do pipeline (PlantUML)
└── src/
    ├── main/kotlin/capsule/
    │   ├── CapsulePlugin.kt             # ponto de entrada — aplica slider, registra tarefas, funde a config
    │   ├── CapsuleManager.kt            # registro de tarefas + fábricas de motores (resolve*)
    │   ├── CapsuleModels.kt             # DSL CapsuleExtension + CapsuleConventions
    │   ├── CapsuleConfig.kt             # config imutável — 18 seções
    │   ├── CapsuleConfigLoader.kt       # carregador YAML com resolução de env ${VAR}
    │   ├── CapsuleConfigMerger.kt       # fusão de 4 fontes (CLI > YAML > gradle.properties > ENV)
    │   ├── CapsuleConfigLogger.kt       # log estruturado da config (CR-8)
    │   ├── Capsule{Messages}*.kt        # resolução de recursos i18n (10 idiomas)
    │   ├── TtsManager.kt                # integração TTS Piper + espeak + Fish Audio
    │   ├── PlaywrightManager.kt         # captura Chromium headless
    │   ├── CaptureResolver.kt           # despacho PLAYWRIGHT / SCREENSHOT / REMOTION
    │   ├── ScreenshotPlanner.kt         # plano puro: PNG por slide → WebM → concat demuxer
    │   ├── Remotion*.kt                 # estratégia de captura Remotion (Node/@remotion/renderer)
    │   ├── Manim{Engine,SlideReplacer,VideoMixer,ParallelRenderer}.kt  # enriquecimento Manim
    │   ├── Mp4ConversionCommand.kt      # argv FFmpeg puro para WebM→MP4 (+ BT.709, opt-in)
    │   ├── VideoFormatConverter.kt      # transcodificação WebM → MP4/H.264 (CAP-MP4)
    │   ├── FormatConversion.kt          # despacho WEBM / MP4 / BOTH
    │   ├── VideoDestinationResolver.kt  # destino versionado (TIMESTAMP / INCREMENTAL)
    │   ├── Subtitle{BurnInCommand,BurnInService,Cartoner,TextNormalizer,
    │   │            TimingCalculator,TimingDistributor,Models}.kt + SrtGenerator.kt + VttGenerator.kt
    │   ├── Html{SectionParser,Escape}.kt
    │   ├── AudioConversionUtil.kt       # conversão WAV→MP3
    │   ├── MediaProbeUtil.kt            # sondagem de duração via ffprobe
    │   ├── RenderSignature.kt           # assinatura de render + reuso .sig (PRT-06)
    │   ├── ProcessRunner.kt / FileReplace.kt / StrictModeGuard.kt
    │   ├── ai/            # ponte LLM para o LlmBuildService do codebase (CapsuleLlmService, capsuleAiSmokeTest)
    │   ├── anim/          # porte do timing Remotion (RemotionTiming)
    │   ├── audio/         # CAP-AUDIO: AudioPostCommand (argv puro), AudioPostProcessor (+NoOp), BGM/loudnorm/ducking
    │   ├── chapters/      # marcadores de capítulo, CardRenderer, YouTubeChaptersFormatter
    │   ├── ci/            # CucumberTestGuard (CR-10), ProcessMigrationGuard, OssVocabularyGuard
    │   ├── context/       # contexto aumentado: CapsuleContext(Builder), loaders Doc/Scenario/Glossary, ProvenanceTracker
    │   ├── feed/          # contrato de extração de notas do orador (SLD-11) + registrars compostos
    │   ├── i18n/          # CapsuleMessages (bundles de 10 idiomas)
    │   ├── multilang/     # CAP-29: MultiLanguageResolver, VoiceMapping, CapsuleVideoPlan(ner), runner AllLanguages
    │   ├── pipeline/      # CAP-ARCH-3: grafo koog CapsulePipelineGraph, CapsuleState, CapsuleLlm (+ChatModel), ContentPlanValidator
    │   ├── podcast/       # CAP-PODCAST: PodcastConcatenator (+Impl/NoOp), PodcastConcatCommand
    │   ├── preview/       # dry-run (capture.previewOnly — apenas PNG)
    │   ├── publish/       # VideoDescriptionBuilder (guarda do YouTube)
    │   ├── qc/            # CAP-QC: ContentPreflight (PRT-05), QcBoard/planejadores + RenderQcBoardsTask (PRT-08)
    │   ├── timeline/      # PRT-07: timeline determinística (quadros inteiros, lead/tail/avance)
    │   ├── transcript/    # CAP-TRANSCRIPT: TranscriptPlan/Builder, ChatModelTranscriptEnhancer
    │   ├── validation/    # VideoDurationValidator + VideoIntegrityChecker (PRT-04)
    │   ├── viral/         # CAP-VIRAL: storyboard, deck builder, campaign bundle/gate, thumbnail, karaokê
    │   └── voice/         # CAP-QC-VOICE: TextNormalizer, WordAligner, FidelityScorer, SnrEstimator,
    │                      #   PronunciationLexicon, NumberSpeller, SpeechRecognizer (porta), TakeSelector
    ├── main/resources/capsule/
    │   ├── i18n/Messages[_xx].properties   # 10 idiomas + base
    │   └── remotion/                        # composição Remotion (Capsule.jsx, Root.jsx, render.mjs)
    ├── test/kotlin/capsule/
    │   ├── scenarios/                       # definições de steps Cucumber + runners por feature
    │   └── **/*Test.kt                      # testes unitários
    ├── test/features/                       # *.feature — cenários Cucumber (44 arquivos)
    └── functionalTest/kotlin/capsule/       # testes funcionais Gradle TestKit
```

## Tarefas Gradle

| Grupo | Tarefas |
|-------|---------|
| Pipeline principal | `generateCapsuleScript`, `generateCapsule` (TTS), `generateCapsuleVideo` (injeção de áudio + captura), `generateCapsuleVideoAllLanguages`, `deployCapsule` (recorte 9:16), `distributeCapsuleVideo` (destino versionado) |
| Conteúdo LLM | `generateCapsuleContent`, `capsuleAiSmokeTest`, `collectCapsuleAugmentedContext`, `generateCapsuleContentAndVideos` |
| Contexto | `collectCapsuleContext`, `transformCapsuleContext`, `collectCapsuleRetrieve`, `scaffoldCapsuleContext` |
| Saída | `generateCapsuleTranscript`, `generateCapsuleChapters`, `generateCapsulePodcast`, `validateCapsuleVideoDuration` |
| Qualidade (opt-in) | `preflightCapsuleContent`, `renderQcBoards`, `verifyCapsuleVoice`, `selectBestVoiceTake` |
| Viral (opt-in) | `generateViralCampaign`, `normalizeViralStoryboard`, `renderViralVariants`, `renderViralThumbnails`, `renderViralSubtitles`, `validateViralCampaign` |
| Composto feed | `translateAndExtractSpeakerNotes`, `translateAndGenerateCapsuleVideos` |

## Dependências

`capsule-gradle` consome `slider-gradle` (`education.cccp:slider:0.0.19`) com `compileOnly` —
um contrato baseado em arquivos (capsule lê o HTML do deck e o texto do script, nunca os modifica).
`CapsulePlugin` aplica automaticamente `education.cccp.slider` em tempo de execução se disponível.

Desde CAP-ARCH-1, capsule também consome o socle LLM do codebase
(`education.cccp:codebase-plugin`) como `implementation` — a ponte `LlmBuildService`
fornece o `ChatModel` usado por `generateCapsuleContent`, com um fallback mock-LLM via
`-Pollama.baseUrl` para os testes (zero rede, zero pool).

| Biblioteca | Versão | Papel |
|------------|--------|-------|
| **Playwright** | 1.52.0 | Chromium headless — controle do reveal.js + captura de vídeo (JVM nativo, zero npm) |
| **koog-agents** | 1.3.0 | DSL Kotlin para orquestração de agentes (StateGraph / ConditionalEdges) — `CapsulePipelineGraph` |
| **langchain4j** | 1.22.0 | Adaptadores `ChatModel` (ponte LLM, enriquecedor de transcrição) — versão via BOM |
| **Jackson** | 2.21.2 | Config YAML + serialização JSON (databind, kotlin, dataformat-yaml) |
| **Kover** | 0.9.8 | Relatórios de cobertura (XML + HTML, integrados no `check`) |
| **Cucumber** | 7.34.3 | Testes BDD (cucumber-java, junit-platform-engine, picocontainer) |
| **Kotlin** | 2.4.10 | DSL do plugin |
| **codebase-plugin** | 0.0.17 | Socle LLM N1 — ponte `LlmBuildService` (`capsule.ai`) |
| **codebase-contracts** | 0.0.2 | Contratos N0 — CompositeContext / ContextChannel / ChannelBudget (`capsule.context`) |
| **i18n-contracts** | 0.0.4 | Contratos N0 — `LanguageCatalog` / `TranslationService` (`capsule.i18n`) |
| **slider** | 0.0.19 | compileOnly — deck reveal.js + fonte do script de capsule |

Ferramentas externas (não são dependências Maven): **Piper** (TTS local offline, padrão),
**espeak-ng** (motor TTS de fallback), **FFmpeg** (recorte, burn-in, MP4, áudio pós),
**Manim** (esquemas animados opcionais, `[manim:SceneName]`), **Chromium** (baixado
automaticamente pelo Playwright na primeira execução), **Node** (estratégia Remotion).

## Configuração

`CapsuleConfig` tem **18 seções**: `input`, `tts`, `capture`, `distrib`, `manim`,
`output`, `strictMode`, `context`, `validation`, `qc`, `audioPost`, `transcript`,
`remotion`, `podcast`, `preview`, `chapters`, `viral`, `voice`.

Cada valor é resolvido a partir de **4 fontes** (precedência **CLI > YAML > gradle.properties > ENV**):

```bash
# CLI
./gradlew generateCapsuleVideo -Poutput.format=mp4 -Pcapture.previewOnly=true

# gradle.properties / ENV
capsule.output.format=mp4
CAPSULE_OUTPUT_FORMAT=mp4
```

Novas capacidades são **opt-in (`false` por padrão)** para compatibilidade retroativa.

## Matriz de testes

| Tarefa | Escopo | Detalhes |
|--------|--------|----------|
| `test` | Testes unitários JUnit5 | Exclui `capsule.scenarios.**`. Os testes `@integration` *não* são filtrados: auto-skip quando falta a ferramenta externa (Chromium, espeak, FFmpeg, Manim) |
| `cucumberTest` | Cucumber BDD | features em `src/test/features` (44 arquivos `.feature`), `forkEvery = 1`, `maxParallelForks = 1`, heap 1g. Ignorado exceto com `-PrunCucumber` ou `CI=true` (CR-10) |
| `functionalTest` | Gradle TestKit | Testes funcionais de aplicação do plugin |
| `check` | Tudo | Depende de `test` + `cucumberTest` + `functionalTest` |

Tags Cucumber: `@ai`, `@architecture`, `@audio`, `@boundary`, `@burnin`, `@capsule`, `@capture`, `@ci`, `@config`, `@context`, `@cross-borough`, `@distrib`, `@docs`, `@duration`, `@feed`, `@format`, `@glossary`, `@i18n`, `@integration`, `@logging`, `@manim`, `@model`, `@multilang`, `@no-yaml`, `@parallel`, `@pipeline`, `@post`, `@provenance`, `@rag`, `@scenario`, `@sequential`, `@strategy`, `@strict`, `@style`, `@subtitles`, `@timeout`, `@transcript`, `@translation`, `@tts`, `@validation`, `@versioning`, `@vtt`, `@wiring`. **38** tarefas `cucumberTest*` dedicadas isolam uma feature cada (`CucumberTaskSpec` em `build.gradle.kts`).

Base de testes: o verde de referência é o workflow CI `Full Tests` em `main`
(unitários + TestKit funcional + Cucumber agregado/render real; o `cucumberTest`
de diagnóstico é ignorado por padrão desde CR-10).

## Pipeline de conteúdo LLM (CAP-ARCH-3)

`generateCapsuleContent` orquestra o `CapsulePipelineGraph` do koog numa única invocação:

```
propose-context → validate-context → generate-speaker-notes
```

- **Entradas**: deck fonte `.adoc` (`-Pdeck.file=<caminho>` ou o primeiro `.adoc` em `slides/misc`),
  contexto aumentado opcional de `collectCapsuleAugmentedContext`
  (`build/capsule/augmented-context.txt`), idioma alvo (`-Pdeck.language`, padrão `fr`).
- **Saídas** (declaradas → Economia de Tinta via UP-TO-DATE): notas do orador enriquecidas
  `build/capsule/<deckName>-speaker-notes.adoc` + script TTS `build/capsule/<deckName>-script.txt`.
- **LLM**: resolvido via o `LlmBuildService` do codebase; os testes injetam um servidor Ollama
  simulado com `-Pollama.baseUrl=http://localhost:<port>` (roteado por prompt: propose → JSON do
  plano de conteúdo, generate → AsciiDoc enriquecido). Zero rede, zero pool nos testes.

```bash
./gradlew generateCapsuleContent \
  -Pdeck.file=slides/misc/my-deck.adoc \
  -Pdeck.language=en
```

## Ajuste da JVM

Os testes Cucumber usam `maxHeapSize = "1g"` com `forkEvery = 1` (JVM nova por cenário)
e `maxParallelForks = 1` (execução sequencial para estabilidade).

```bash
export GRADLE_OPTS="-Xmx2g"   # builds locais
```

## Comandos de build

```bash
./gradlew build                       # build completo (compila + testes)
./gradlew build -x test               # só compilação
./gradlew check                       # todos os testes (unitários + Cucumber + funcionais)
./gradlew test                        # só unitários JUnit5
./gradlew cucumberTest -PrunCucumber  # Cucumber BDD (runner agregado)
./gradlew functionalTest              # Gradle TestKit
./gradlew koverReport                 # relatórios de cobertura (XML + HTML)
./gradlew publishToMavenLocal         # publicação local
```

## Pipeline CI

Quatro workflows em `.github/workflows/`:

| Workflow | Gatilho | Job |
|----------|---------|-----|
| `test.yml` — Fast Tests (portão PR) | push / PR | JDK 25 + Gradle em cache → `./gradlew test` (unitários) |
| `full-tests.yml` — Full Tests | push | ffmpeg + espeak, **Chromium Playwright em cache**, Gradle em cache → `./gradlew test functionalTest`, depois `./gradlew cucumberTest cucumberTestViralRender -PrunCucumber` |
| `cucumber-full.yml` — diagnóstico | manual (`workflow_dispatch`) | `cucumberTest` completo (não bloqueante) |
| `llm-smoke.yml` — Ollama smoke | manual | imagem + modelo Ollama em cache, verifica que o modelo responde |

Cache: `gradle/actions/setup-gradle@v4` (wrapper + dependências + build cache),
`.github/actions/setup-playwright` (Chromium, chave derivada da versão `playwright` do
catálogo), `.github/actions/setup-ollama` (imagem + modelos).

## Publicação (NMCP)

**Publicado no Maven Central** — versão `0.0.10` (anteriores `0.0.7`, `0.0.6`, `0.0.5`, …).
O POM é configurado com `maven-publish` e `signing`:

- POM: nome "Capsule Gradle Plugin", descrição "Generation automatisee de capsules video
  depuis des decks reveal.js", URL `github.com/cheroliv/capsule-gradle/`
- Assinatura: `useGpgCmd()` (executada na publicação)
- Agregação NMCP: `publishAggregationToCentralPortal` (credenciais em `~/.gradle/gradle.properties`)

```bash
./gradlew publishToMavenLocal
./gradlew publishAggregationToCentralPortal --no-daemon
```

## Documentos de arquitetura e governança

- [doc/CAPSULE_ARCHITECTURE.adoc](../doc/CAPSULE_ARCHITECTURE.adoc) — arquitetura do pipeline (PlantUML)
- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPICs, roadmap e painel de governança
- [AGENT.adoc](../AGENT.adoc) — regras absolutas (incl. a Lei da Economia de Tinta)
- [BACKLOG.adoc](../BACKLOG.adoc) — estado detalhado dos EPICs

Todos os EPICs estão fechados: `CAP-VIRAL` (14/14), `CAP-SHORT` (6/6), `CAP-VIRAL-E2E` (3/3),
`CAP-CONTEXT` (4/4), `CAP-QC` (9/9), `CAP-QC-VOICE` (5/5), `LLM-RESILIENCE` (1/1),
`CAP-ARCH`, `CAP-29`, `CAP-MP4`, `CAP-AUDIO`, `CAP-TRANSCRIPT`, `CAP-PODCAST`,
`CAP-GLOSSARY`, `CAP-PROVENANCE`, `CAP-27`, `CAP-28`, `RENAME`, e o conjunto de revisão
CR-1→CR-10. O backlog acionável está vazio (ver `.agents/INDEX.adoc`).

## Contribuir

1. O build compila: `./gradlew build -x test`
2. Todos os testes verdes: `./gradlew check` (ou `-PrunCucumber` para BDD)
3. Respeitar as regras absolutas (ver [AGENT.adoc](../AGENT.adoc)):
   - Sem commits/push sem permissão
   - DAG N2 — importável por N3, nunca importa N3
   - Capsule consome slider (somente leitura, nunca modifica o deck fonte)
   - Zero segredos/tokens no código
4. Respeitar a Lei da Economia de Tinta — nunca re-executar uma operação custosa
   (TTS, captura Playwright) quando já existe um artefato válido para a mesma entrada
5. Baby-step DDD/TDD/BDD — um arquivo de cada vez; unitário → funcional → Cucumber

## Licença

Apache License 2.0 — ver [LICENSE](../../LICENSE).

---

_Parte do ecossistema CCCP Education — `groupId: education.cccp`._
