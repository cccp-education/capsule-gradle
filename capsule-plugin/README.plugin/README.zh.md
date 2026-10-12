<!-- translated from README.md rev 0.0.10 -->
# capsule-gradle — 插件内部

> `capsule-plugin` Gradle 插件的开发者与贡献者指南。

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.10&color=green)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![覆盖率](https://img.shields.io/static/v1?label=%E8%A6%86%E7%9B%96%E7%8E%87&message=Kover&color=blue)]()
[![许可证](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=%E8%AE%B8%E5%8F%AF%E8%AF%81)](../../LICENSE)

- **版本**：`0.0.10`（Maven Central） · **组**：`education.cccp` · **插件 ID**：`education.cccp.capsule`
- **工具链**：Java 25 · Kotlin 2.4.10 · Gradle 9.8.1
- **构建**：`./gradlew build -x test` · **测试**：`./gradlew check`（单元 + 通过 TestKit 的功能测试；Cucumber 加 `-PrunCucumber`） · **覆盖率**：`./gradlew koverReport`

🌐 语言：[English](../README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## 功能

`capsule-gradle` 将 reveal.js 演示文稿（由 `slider-gradle` 生成）转换为同步视频胶囊——
每张幻灯片的 TTS 配音、视频捕获、字幕、格式转换、音频后期制作、质量控制以及
垂直短视频/病毒式流水线——无需人工后期制作。它是 N2 级 Gradle 插件
（由 N3 runner 消费，自身绝不是 N3）。

## 模块结构

```
capsule-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml            # 内部版本目录
├── doc/CAPSULE_ARCHITECTURE.adoc        # 流水线架构（PlantUML）
└── src/
    ├── main/kotlin/capsule/
    │   ├── CapsulePlugin.kt             # 入口——应用 slider、注册任务、合并配置
    │   ├── CapsuleManager.kt            # 任务注册 + 引擎工厂（resolve*）
    │   ├── CapsuleModels.kt             # CapsuleExtension DSL + CapsuleConventions
    │   ├── CapsuleConfig.kt             # 不可变配置——18 个部分
    │   ├── CapsuleConfigLoader.kt       # 带 ${VAR} 环境变量解析的 YAML 加载器
    │   ├── CapsuleConfigMerger.kt       # 4 源合并（CLI > YAML > gradle.properties > ENV）
    │   ├── CapsuleConfigLogger.kt       # 结构化配置日志（CR-8）
    │   ├── Capsule{Messages}*.kt        # i18n 资源解析（10 种语言）
    │   ├── TtsManager.kt                # Piper + espeak + Fish Audio TTS 集成
    │   ├── PlaywrightManager.kt         # 无头 Chromium 捕获
    │   ├── CaptureResolver.kt           # PLAYWRIGHT / SCREENSHOT / REMOTION 分发
    │   ├── ScreenshotPlanner.kt         # 纯计划：每张幻灯片 PNG → WebM → concat demuxer
    │   ├── Remotion*.kt                 # Remotion 捕获策略（Node/@remotion/renderer）
    │   ├── Manim{Engine,SlideReplacer,VideoMixer,ParallelRenderer}.kt  # Manim 富化
    │   ├── Mp4ConversionCommand.kt      # WebM→MP4 的纯 FFmpeg argv（+ BT.709，可选）
    │   ├── VideoFormatConverter.kt      # WebM → MP4/H.264 转码（CAP-MP4）
    │   ├── FormatConversion.kt          # WEBM / MP4 / BOTH 分发
    │   ├── VideoDestinationResolver.kt  # 版本化目标（TIMESTAMP / INCREMENTAL）
    │   ├── Subtitle{BurnInCommand,BurnInService,Cartoner,TextNormalizer,
    │   │            TimingCalculator,TimingDistributor,Models}.kt + SrtGenerator.kt + VttGenerator.kt
    │   ├── Html{SectionParser,Escape}.kt
    │   ├── AudioConversionUtil.kt       # WAV→MP3 转换
    │   ├── MediaProbeUtil.kt            # 通过 ffprobe 探测时长
    │   ├── RenderSignature.kt           # 渲染签名 + .sig 复用（PRT-06）
    │   ├── ProcessRunner.kt / FileReplace.kt / StrictModeGuard.kt
    │   ├── ai/            # 通往 codebase LlmBuildService 的 LLM 桥（CapsuleLlmService, capsuleAiSmokeTest）
    │   ├── anim/          # Remotion 计时移植（RemotionTiming）
    │   ├── audio/         # CAP-AUDIO: AudioPostCommand（纯 argv）、AudioPostProcessor（+NoOp）、BGM/loudnorm/ducking
    │   ├── chapters/      # 章节标记、CardRenderer、YouTubeChaptersFormatter
    │   ├── ci/            # CucumberTestGuard（CR-10）、ProcessMigrationGuard、OssVocabularyGuard
    │   ├── context/       # 增强上下文：CapsuleContext(Builder)、Doc/Scenario/Glossary 加载器、ProvenanceTracker
    │   ├── feed/          # 演讲者备注提取契约（SLD-11）+ 组合注册器
    │   ├── i18n/          # CapsuleMessages（10 种语言资源）
    │   ├── multilang/     # CAP-29：MultiLanguageResolver、VoiceMapping、CapsuleVideoPlan(ner)、AllLanguages runner
    │   ├── pipeline/      # CAP-ARCH-3：koog CapsulePipelineGraph、CapsuleState、CapsuleLlm（+ChatModel）、ContentPlanValidator
    │   ├── podcast/       # CAP-PODCAST：PodcastConcatenator（+Impl/NoOp）、PodcastConcatCommand
    │   ├── preview/       # dry-run（capture.previewOnly——仅 PNG）
    │   ├── publish/       # VideoDescriptionBuilder（YouTube 防护）
    │   ├── qc/            # CAP-QC：ContentPreflight（PRT-05）、QcBoard/规划器 + RenderQcBoardsTask（PRT-08）
    │   ├── timeline/      # PRT-07：确定性时间线（整数帧、lead/tail/avance）
    │   ├── transcript/    # CAP-TRANSCRIPT：TranscriptPlan/Builder、ChatModelTranscriptEnhancer
    │   ├── validation/    # VideoDurationValidator + VideoIntegrityChecker（PRT-04）
    │   ├── viral/         # CAP-VIRAL：storyboard、deck builder、campaign bundle/gate、thumbnail、卡拉OK
    │   └── voice/         # CAP-QC-VOICE：TextNormalizer、WordAligner、FidelityScorer、SnrEstimator、
    │                      #   PronunciationLexicon、NumberSpeller、SpeechRecognizer（端口）、TakeSelector
    ├── main/resources/capsule/
    │   ├── i18n/Messages[_xx].properties   # 10 种语言 + 基础
    │   └── remotion/                        # Remotion 合成（Capsule.jsx、Root.jsx、render.mjs）
    ├── test/kotlin/capsule/
    │   ├── scenarios/                       # Cucumber 步骤定义 + 按 feature 的 runner
    │   └── **/*Test.kt                      # 单元测试
    ├── test/features/                       # *.feature — Cucumber 场景（44 个文件）
    └── functionalTest/kotlin/capsule/       # Gradle TestKit 功能测试
```

## Gradle 任务

| 组 | 任务 |
|----|------|
| 主管道 | `generateCapsuleScript`、`generateCapsule`（TTS）、`generateCapsuleVideo`（音频注入 + 捕获）、`generateCapsuleVideoAllLanguages`、`deployCapsule`（9:16 重新裁切）、`distributeCapsuleVideo`（版本化目标） |
| LLM 内容 | `generateCapsuleContent`、`capsuleAiSmokeTest`、`collectCapsuleAugmentedContext`、`generateCapsuleContentAndVideos` |
| 上下文 | `collectCapsuleContext`、`transformCapsuleContext`、`collectCapsuleRetrieve`、`scaffoldCapsuleContext` |
| 输出 | `generateCapsuleTranscript`、`generateCapsuleChapters`、`generateCapsulePodcast`、`validateCapsuleVideoDuration` |
| 质量（可选） | `preflightCapsuleContent`、`renderQcBoards`、`verifyCapsuleVoice`、`selectBestVoiceTake` |
| 病毒式（可选） | `generateViralCampaign`、`normalizeViralStoryboard`、`renderViralVariants`、`renderViralThumbnails`、`renderViralSubtitles`、`validateViralCampaign` |
| 组合 feed | `translateAndExtractSpeakerNotes`、`translateAndGenerateCapsuleVideos` |

## 依赖

`capsule-gradle` 通过 `compileOnly` 消费 `slider-gradle`（`education.cccp:slider:0.0.19`）——
一种基于文件的契约（capsule 读取演示文稿 HTML 与脚本文本，绝不修改它们）。
`CapsulePlugin` 在运行时若可用则自动应用 `education.cccp.slider`。

自 CAP-ARCH-1 起，capsule 还以 `implementation` 消费 codebase LLM 基础
（`education.cccp:codebase-plugin`）——`LlmBuildService` 桥提供 `generateCapsuleContent`
所用的 `ChatModel`，测试中通过 `-Pollama.baseUrl` 使用 mock-LLM 回退（零网络、零池）。

| 库 | 版本 | 作用 |
|----|------|------|
| **Playwright** | 1.52.0 | 无头 Chromium——reveal.js 控制 + 视频捕获（原生 JVM，零 npm） |
| **koog-agents** | 1.3.0 | 智能体编排的 Kotlin DSL（StateGraph / ConditionalEdges）——`CapsulePipelineGraph` |
| **langchain4j** | 1.22.0 | `ChatModel` 适配器（LLM 桥、transcript 增强器）——版本经 BOM |
| **Jackson** | 2.21.2 | YAML 配置 + JSON 序列化（databind、kotlin、dataformat-yaml） |
| **Kover** | 0.9.8 | 覆盖率报告（XML + HTML，接入 `check`） |
| **Cucumber** | 7.34.3 | BDD 测试（cucumber-java、junit-platform-engine、picocontainer） |
| **Kotlin** | 2.4.10 | 插件 DSL |
| **codebase-plugin** | 0.0.17 | N1 LLM 基础——`LlmBuildService` 桥（`capsule.ai`） |
| **codebase-contracts** | 0.0.2 | N0 契约——CompositeContext / ContextChannel / ChannelBudget（`capsule.context`） |
| **i18n-contracts** | 0.0.4 | N0 契约——`LanguageCatalog` / `TranslationService`（`capsule.i18n`） |
| **slider** | 0.0.19 | compileOnly——reveal.js 演示文稿 + capsule 脚本源 |

外部工具（非 Maven 依赖）：**Piper**（本地离线 TTS，默认）、**espeak-ng**（TTS 回退引擎）、
**FFmpeg**（重新裁切、burn-in、MP4、音频后期）、**Manim**（可选动画示意图，`[manim:SceneName]`）、
**Chromium**（首次运行时由 Playwright 自动下载）、**Node**（Remotion 策略）。

## 配置

`CapsuleConfig` 有 **18 个部分**：`input`、`tts`、`capture`、`distrib`、`manim`、
`output`、`strictMode`、`context`、`validation`、`qc`、`audioPost`、`transcript`、
`remotion`、`podcast`、`preview`、`chapters`、`viral`、`voice`。

每个值都从 **4 个来源**解析（优先级 **CLI > YAML > gradle.properties > ENV**）：

```bash
# CLI
./gradlew generateCapsuleVideo -Poutput.format=mp4 -Pcapture.previewOnly=true

# gradle.properties / ENV
capsule.output.format=mp4
CAPSULE_OUTPUT_FORMAT=mp4
```

新能力均为 **opt-in（默认 `false`）** 以保持向后兼容。

## 测试矩阵

| 任务 | 范围 | 说明 |
|------|------|------|
| `test` | JUnit5 单元测试 | 排除 `capsule.scenarios.**`。`@integration` 测试*不*被过滤：当外部工具（Chromium、espeak、FFmpeg、Manim）缺失时会自行跳过 |
| `cucumberTest` | Cucumber BDD | `src/test/features` 下的 feature（44 个 `.feature` 文件），`forkEvery = 1`、`maxParallelForks = 1`、堆 1g。除非 `-PrunCucumber` 或 `CI=true` 否则跳过（CR-10） |
| `functionalTest` | Gradle TestKit | 插件应用功能测试 |
| `check` | 全部 | 依赖 `test` + `cucumberTest` + `functionalTest` |

Cucumber 标签：`@ai`、`@architecture`、`@audio`、`@boundary`、`@burnin`、`@capsule`、`@capture`、`@ci`、`@config`、`@context`、`@cross-borough`、`@distrib`、`@docs`、`@duration`、`@feed`、`@format`、`@glossary`、`@i18n`、`@integration`、`@logging`、`@manim`、`@model`、`@multilang`、`@no-yaml`、`@parallel`、`@pipeline`、`@post`、`@provenance`、`@rag`、`@scenario`、`@sequential`、`@strategy`、`@strict`、`@style`、`@subtitles`、`@timeout`、`@transcript`、`@translation`、`@tts`、`@validation`、`@versioning`、`@vtt`、`@wiring`。**38** 个专用 `cucumberTest*` 任务各自隔离一个 feature（`build.gradle.kts` 中的 `CucumberTaskSpec`）。

测试基线：权威的绿色是 `main` 上的 CI `Full Tests` 工作流
（单元 + TestKit 功能 + 聚合/真实渲染 Cucumber；诊断用的 `cucumberTest`
自 CR-10 起默认跳过）。

## LLM 内容流水线（CAP-ARCH-3）

`generateCapsuleContent` 在一次调用中编排 koog `CapsulePipelineGraph`：

```
propose-context → validate-context → generate-speaker-notes
```

- **输入**：源演示文稿 `.adoc`（`-Pdeck.file=<路径>` 或 `slides/misc` 中第一个 `.adoc`）、
  来自 `collectCapsuleAugmentedContext` 的可选增强上下文
  （`build/capsule/augmented-context.txt`）、目标语言（`-Pdeck.language`，默认 `fr`）。
- **输出**（声明 → 经 UP-TO-DATE 实现"墨水经济"）：增强的演讲者备注
  `build/capsule/<deckName>-speaker-notes.adoc` + TTS 脚本 `build/capsule/<deckName>-script.txt`。
- **LLM**：通过 codebase 的 `LlmBuildService` 解析；测试注入模拟 Ollama 服务器，
  使用 `-Pollama.baseUrl=http://localhost:<port>`（按提示路由：propose → 内容计划 JSON，
  generate → 增强 AsciiDoc）。测试中零网络、零池。

```bash
./gradlew generateCapsuleContent \
  -Pdeck.file=slides/misc/my-deck.adoc \
  -Pdeck.language=en
```

## JVM 调优

Cucumber 测试使用 `maxHeapSize = "1g"`、`forkEvery = 1`（每个场景一个新 JVM）
和 `maxParallelForks = 1`（顺序执行以保持稳定）。

```bash
export GRADLE_OPTS="-Xmx2g"   # 本地构建
```

## 构建命令

```bash
./gradlew build                       # 完整构建（编译 + 测试）
./gradlew build -x test               # 仅编译
./gradlew check                       # 所有测试（单元 + Cucumber + 功能）
./gradlew test                        # 仅 JUnit5 单元
./gradlew cucumberTest -PrunCucumber  # Cucumber BDD（聚合 runner）
./gradlew functionalTest              # Gradle TestKit
./gradlew koverReport                 # 覆盖率报告（XML + HTML）
./gradlew publishToMavenLocal         # 本地发布
```

## CI 流水线

`.github/workflows/` 下四个工作流：

| 工作流 | 触发 | 作业 |
|--------|------|------|
| `test.yml` — Fast Tests（PR 门禁） | push / PR | JDK 25 + Gradle 缓存 → `./gradlew test`（单元） |
| `full-tests.yml` — Full Tests | push | ffmpeg + espeak、**Playwright Chromium 缓存**、Gradle 缓存 → `./gradlew test functionalTest`，然后 `./gradlew cucumberTest cucumberTestViralRender -PrunCucumber` |
| `cucumber-full.yml` — 诊断 | 手动（`workflow_dispatch`） | 完整 `cucumberTest`（非阻断） |
| `llm-smoke.yml` — Ollama smoke | 手动 | 缓存 Ollama 镜像 + 模型，验证模型可应答 |

缓存：`gradle/actions/setup-gradle@v4`（wrapper + 依赖 + 构建缓存）、
`.github/actions/setup-playwright`（Chromium，键取自目录中的 `playwright` 版本）、
`.github/actions/setup-ollama`（镜像 + 模型）。

## 发布（NMCP）

**已发布到 Maven Central** — 版本 `0.0.10`（此前 `0.0.7`、`0.0.6`、`0.0.5`……）。
POM 通过 `maven-publish` 与 `signing` 配置：

- POM：name "Capsule Gradle Plugin"，description "Generation automatisee de capsules video
  depuis des decks reveal.js"，URL `github.com/cheroliv/capsule-gradle/`
- 签名：`useGpgCmd()`（发布时执行）
- NMCP 聚合：`publishAggregationToCentralPortal`（凭据在 `~/.gradle/gradle.properties`）

```bash
./gradlew publishToMavenLocal
./gradlew publishAggregationToCentralPortal --no-daemon
```

## 架构与治理文档

- [doc/CAPSULE_ARCHITECTURE.adoc](../doc/CAPSULE_ARCHITECTURE.adoc) — 流水线架构（PlantUML）
- [.agents/INDEX.adoc](../.agents/INDEX.adoc) — EPIC、路线图与治理面板
- [AGENT.adoc](../AGENT.adoc) — 绝对规则（含"墨水经济"法则）
- [BACKLOG.adoc](../BACKLOG.adoc) — EPIC 详细状态

所有 EPIC 均已关闭：`CAP-VIRAL`（14/14）、`CAP-SHORT`（6/6）、`CAP-VIRAL-E2E`（3/3）、
`CAP-CONTEXT`（4/4）、`CAP-QC`（9/9）、`CAP-QC-VOICE`（5/5）、`LLM-RESILIENCE`（1/1）、
`CAP-ARCH`、`CAP-29`、`CAP-MP4`、`CAP-AUDIO`、`CAP-TRANSCRIPT`、`CAP-PODCAST`、
`CAP-GLOSSARY`、`CAP-PROVENANCE`、`CAP-27`、`CAP-28`、`RENAME`，以及评审集 CR-1→CR-10。
可执行的待办为空（见 `.agents/INDEX.adoc`）。

## 贡献

1. 构建可编译：`./gradlew build -x test`
2. 所有测试通过：`./gradlew check`（BDD 用 `-PrunCucumber`）
3. 遵守绝对规则（见 [AGENT.adoc](../AGENT.adoc)）：
   - 未经许可不得 commit/push
   - DAG N2——可被 N3 导入，绝不导入 N3
   - Capsule 消费 slider（只读，绝不修改源演示文稿）
   - 代码中零密钥/令牌
4. 遵守"墨水经济"法则——当同一输入的有效产物已存在时，绝不重跑昂贵操作
   （TTS、Playwright 捕获）
5. Baby-step DDD/TDD/BDD——一次一个文件；单元 → 功能 → Cucumber

## 许可证

Apache License 2.0——见 [LICENSE](../../LICENSE)。

---

_CCCP Education 生态系统的一部分——`groupId: education.cccp`。_
