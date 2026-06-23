<!-- translated from README.md rev 0.0.1-SNAPSHOT -->
# capsule-gradle — Interior do Plugin

> Guia para programadores e contribuintes do plugin Gradle `capsule-plugin`.

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.1-SNAPSHOT&color=orange)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=coverage&message=Kover&color=blue)]()
[![License](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=License)](../LICENSE)

- **Versão**: `0.0.1-SNAPSHOT` (não publicado) · **Grupo**: `education.cccp` · **ID do plugin**: `education.cccp.capsule`
- **Toolchain**: Java 24 · Kotlin 2.3.20 · Gradle 9.5
- **Build**: `./gradlew build -x test` · **Testes**: `./gradlew check` (195 unitários + 22 Cucumber PASS) · **Cobertura**: `./gradlew koverReport`

🌐 Languages: [English](README.md) | [中文](README.zh.md) | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | **Português** | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## Disposição dos módulos

```
capsule-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml          # catálogo de versões
├── doc/
│   └── CAPSULE_ARCHITECTURE.adoc      # arquitetura do pipeline (PlantUML)
└── src/
    ├── main/kotlin/capsule/
    │   ├── CapsulePlugin.kt           # ponto de entrada — aplica slider, regista tarefas, fusão de config
    │   ├── CapsuleManager.kt          # registo de tarefas + parsing de script + métodos factory
    │   ├── CapsuleModels.kt           # DSL CapsuleExtension + CapsuleConventions + modelos de dados
    │   ├── CapsuleConfig.kt           # config imutável (5 secções: input, tts, capture, distrib, manim)
    │   ├── CapsuleConfigLoader.kt     # carregador YAML com resolução de env ${VAR}
    │   ├── CapsuleConfigMerger.kt     # fusão de 4 fontes (CLI > YAML > props > ENV)
    │   ├── CapsuleScriptTask.kt       # generateCapsuleScript
    │   ├── CapsuleBuildTask.kt        # generateCapsule (síntese TTS)
    │   ├── CapsuleVideoTask.kt        # generateCapsuleVideo (injeção de áudio + captura Playwright)
    │   ├── CapsuleDistribTask.kt     # deployCapsule (recorte 9:16 FFmpeg)
    │   ├── CapsuleCompositeContextTask.kt  # collectCapsuleContext (exportação JSON N3)
    │   ├── CapsuleParseContextTask.kt # transformCapsuleContext + collectCapsuleRetrieve
    │   ├── CapsuleScaffoldTask.kt    # scaffoldCapsuleContext
    │   ├── TtsManager.kt             # integração TTS Piper + espeak
    │   ├── PlaywrightManager.kt      # captura Chromium headless
    │   ├── ManimEngine.kt            # renderização Manim (NoOp/Impl)
    │   ├── ManimSlideReplacer.kt     # substituição HTML→vídeo embed
    │   ├── ManimVideoMixer.kt        # mux MP4 Manim + áudio TTS
    │   ├── ManimParallelRenderer.kt  # renderização Manim paralela
    │   ├── SubtitleModels.kt         # modelos de dados de legendas
    │   ├── SubtitleTimingCalculator.kt
    │   ├── SrtGenerator.kt           # geração de legendas SRT
    │   ├── VttGenerator.kt           # geração de legendas VTT
    │   ├── SubtitleBurnInService.kt  # burn-in de legendas FFmpeg
    │   ├── HtmlSectionParser.kt       # parsing HTML de apresentação reveal.js
    │   ├── AudioConversionUtil.kt    # conversão WAV→MP3
    │   ├── MediaProbeUtil.kt         # probing de duração ffprobe
    │   └── LanguageVoiceMapping.kt    # mapeamento de vozes FR/EN/ES/DE
    ├── test/kotlin/capsule/
    │   ├── CapsulePluginTest.kt
    │   ├── scenarios/                 # step definitions Cucumber BDD + runners
    │   │   ├── CapsuleStepDefinitions.kt
    │   │   ├── CucumberTestRunner.kt
    │   │   └── CucumberIntegrationTestRunner.kt
    │   └── *Test.kt                   # 17 classes de testes unitários
    ├── test/features/
    │   └── capsule.feature            # 22 cenários Cucumber
    └── functionalTest/kotlin/capsule/
        └── CapsulePluginFunctionalTest.kt
```

## Dependência N2

`capsule-gradle` consome `slider-gradle` (`education.cccp:slider:0.0.6`) via `compileOnly`.
O contrato é baseado em ficheiros — capsule lê o HTML da apresentação e os ficheiros de texto
de script produzidos pelo slider, nunca os modifica. CapsulePlugin aplica automaticamente
`education.cccp.slider` em runtime se disponível.

## Bibliotecas principais

| Biblioteca | Versão | Papel |
|---------|---------|------|
| **Playwright** | 1.52.0 | Chromium headless — controlo reveal.js + captura de vídeo (JVM nativo, zero npm) |
| **koog-agents** | 0.8.0 | DSL Kotlin para orquestração de agentes (StateGraph / ConditionalEdges) |
| **Jackson** | 2.18.3 | Config YAML + serialização JSON (databind, kotlin, dataformat-yaml) |
| **Kover** | 0.9.8 | Relatórios de cobertura (XML + HTML, integrados em `check`) |
| **Cucumber** | 7.34.3 | Testes BDD (cucumber-java, junit-platform-engine, picocontainer) |
| **Kotlin** | 2.3.20 | DSL do plugin |
| **slider** | 0.0.6 | compileOnly — apresentação reveal.js + script cápsula fonte |

Ferramentas externas (não dependências Maven):
- **Piper** — motor TTS local offline (padrão)
- **espeak** — motor TTS de recurso
- **FFmpeg** — recorte 9:16 (`deployCapsule`) e burn-in de legendas
- **Manim** — renderização de animação matemática (opcional, `[manim:SceneName]` no script)
- **Chromium** — auto-descarregado pelo Playwright na primeira execução

## Matriz de testes

| Tarefa | Âmbito | Detalhes |
|------|-------|---------|
| `test` | Testes unitários JUnit5 | Exclui a etiqueta `@integration` e `capsule.scenarios.**` |
| `cucumberTest` | BDD Cucumber | 22 cenários, `forkEvery = 1`, `maxParallelForks = 1`, heap 1g |
| `functionalTest` | Gradle TestKit | Testes funcionais de aplicação do plugin |
| `check` | Todos | Depende de `test` + `cucumberTest` + `functionalTest` |

Etiquetas Cucumber: `@integration`, `@manim`, `@config`, `@tts`, `@subtitles`, `@burnin`, `@style`, `@parallel`。

Total de testes: **195 unitários + 22 Cucumber = 217 PASS** (baseline sessão 029)。

## Ajuste JVM

Os testes Cucumber usam `maxHeapSize = "1g"` com `forkEvery = 1` (JVM nova por cenário)
e `maxParallelForks = 1` (execução sequencial para estabilidade).

Para builds locais:
```bash
export GRADLE_OPTS="-Xmx2g"
```

## Comandos de build

```bash
./gradlew build                       # build completo (compila + testa)
./gradlew build -x test               # só compilar
./gradlew check                       # todos os testes (unitários + Cucumber + funcionais)
./gradlew test                        # só JUnit5 unitários
./gradlew cucumberTest                # BDD Cucumber
./gradlew functionalTest              # Gradle TestKit
./gradlew koverReport                 # relatórios de cobertura (XML + HTML)
./gradlew publishToMavenLocal         # publicação local
```

## Pipeline CI

`.github/workflows/test.yml` define um job:
1. **Build & Test** — `./gradlew check` em cada push/PR para main/master (≤ 15 min, JDK 24 Temurin, action setup Gradle)

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

## Publicação (NMCP)

**Não publicado** — versão `0.0.1-SNAPSHOT`. O POM está configurado para Maven Central via
os plugins `maven-publish` e `signing`:

- POM: nome "Capsule Gradle Plugin", descrição "Generation automatisee de capsules video
  pedagogiques depuis des decks reveal.js", URL `github.com/cheroliv/capsule-gradle/`
- Assinatura: `useGpgCmd()` (configurado mas ainda não executado)
- Alvo de publicação: `publishToMavenLocal` disponível; NMCP `publishAggregationToCentralPortal`
  requer credenciais em `~/.gradle/gradle.properties`

Para publicar quando estiver pronto:
```bash
./gradlew publishAggregationToCentralPortal --no-daemon
```

## Documentos de arquitetura

- [CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) — Arquitetura
  do pipeline com diagramas PlantUML (pipeline global, componentes, sequência, implantação)
- [.agents/INDEX.adoc](../capsule-plugin/.agents/INDEX.adoc) — EPICs, roadmap e governação
- [AGENT.adoc](../capsule-plugin/AGENT.adoc) — Regras absolutas (7 regras, Lei da Economia de Tinta)
- [CODE_REVIEW.adoc](../capsule-plugin/CODE_REVIEW.adoc) — Code review (10 EPICs acionáveis CR-1→CR-10)

## Estado dos EPICs

CAP-0 a CAP-25 terminados. EPICs ativos (ver `.agents/INDEX.adoc`):

| EPIC | Descrição | Estado |
|------|-------------|--------|
| CR-1 | Refatorizar `CapsuleVideoTask.execute()` (218→76 linhas) | ✅ Terminado (sessão 040) |
| CR-2 | Thread safety + timeout `captureSlideParallel` | ⬜ TODO |
| CR-3 | Tratamento de erros TTS — degradação assinalada | ⬜ TODO |
| CR-4 | Segurança HTML escape — `injectSubtitleTrack` | ⬜ TODO |
| CR-5 | DRY `CapsuleConfigMerger` (~76 linhas duplicadas) | ⬜ TODO |
| CAP-27 | VTT burn-in (atualmente só SRT) | ⬜ TODO |
| CAP-28 | Gaps de cobertura — PiperTtsEngine 31%, ManimEngineImpl 25% | ⬜ TODO |

## Contribuir

1. O build compila: `./gradlew build -x test`
2. Todos os testes verdes: `./gradlew check`
3. Seguir as 7 regras absolutas (ver [AGENT.adoc](../capsule-plugin/AGENT.adoc)):
   - Sem commits/push sem permissão
   - DAG N2 — importável por N3, nunca importa N3
   - Capsule consome slider (só leitura, nunca modifica a apresentação fonte)
   - Zero segredos/tokens no código
4. Respeitar a Lei da Economia de Tinta — nunca re-executar operações dispendiosas (TTS, captura Playwright)
   quando já existe um artefacto válido para a mesma entrada

## Licença

Apache License 2.0 — consulte [LICENSE](../LICENSE)。

---

_Parte do ecossistema CCCP Education — `groupId: education.cccp`._