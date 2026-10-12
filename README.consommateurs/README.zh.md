<!-- translated from README.md rev 0.0.8 -->
# capsule-gradle — 用户指南

> 将 reveal.js 演示文稿转换为同步视频胶囊的 Gradle 插件——TTS 配音、捕获、字幕、格式转换、音频后期以及短视频/病毒式流水线，无需人工后期制作。

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.8&color=green)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![许可证](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=%E8%AE%B8%E5%8F%AF%E8%AF%81)](../LICENSE)

- **版本**：`0.0.8`（Maven Central） · **组**：`education.cccp` · **插件 ID**：`education.cccp.capsule`
- **要求**：Java 25+ · Gradle 9.8+
- **文档**：[插件内部](../capsule-plugin/README.md) · [流水线架构](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc)

🌐 语言：[English](../README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## 功能

`capsule-gradle` 消费 reveal.js 演示文稿（由 `slider-gradle` 生成），产出同步视频胶囊——
每张幻灯片一段 TTS 配音、视频捕获、字幕、格式转换、音频后期制作、质量控制以及可选的
垂直短视频（病毒式）流水线。

```
AsciiDoc 源 → slider-gradle → reveal.js 演示文稿 + 演讲者脚本
                                        ↓
        capsule-gradle → TTS（Piper/espeak）→ 音频注入 → 捕获 → WebM/MP4
                                        ↓                        ↓
                              Manim 动画（可选）   通过 FFmpeg 的 9:16 重新裁切（可选）
```

CCCP Education 多插件生态系统的一部分（`groupId: education.cccp`）。

## 要求

- **Java** 25+（Kotlin 2.4.10 工具链）
- **Gradle** 9.8+
- **Piper** — 本地离线 TTS（默认引擎）
- **espeak / espeak-ng** — TTS 回退（当缺少 `espeak` 时自动使用 `espeak-ng`）
- **Chromium** — 首次运行时由 Playwright 自动下载
- **FFmpeg** — 9:16 重新裁切、字幕烧录、MP4 转换、音频后期
- **Manim** — 可选，用于动画数学幻灯片
- **Node** — 可选，用于 Remotion 捕获/动画策略

## 快速开始

### 1. 应用插件

```gradle
plugins {
    id("education.cccp.capsule") version "0.0.8"
}
```

### 2. 配置

```gradle
capsule {
    outputDir.set("capsules")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    ttsEngine.set("espeak")   // "piper"（默认）或 "espeak"
}
```

### 3. 生成视频

```bash
./gradlew generateCapsuleVideo
```

这将运行完整流水线：脚本校验 → TTS 合成 → 将音频注入演示文稿 HTML → 无头捕获 → 视频输出。

### 与 slider 的完整流水线

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
./gradlew generateCapsuleVideo    # slider 演示文稿 + 脚本 → 视频胶囊
```

## 可用任务

| 任务 | 组 | 说明 |
|------|----|------|
| `generateCapsuleScript` | generate | 读取 slider 生成的 `*-script.txt` 并校验胶囊脚本 |
| `generateCapsule` | generate | 合成 TTS 音频文件（Piper/espeak），每张幻灯片一个 |
| `generateCapsuleVideo` | generate | 将 TTS 音频注入演示文稿 HTML，然后捕获视频 |
| `generateCapsuleVideoAllLanguages` | generate | 每个目标语言一个本地化胶囊（`LanguageCatalog`） |
| `extractSpeakerNotes` | capsule | 将 AsciiDoc 演讲者备注解析为纯文本 `*-script.txt` 契约 |
| `translateAndExtractSpeakerNotes` | capsule | 组合：`translateDeck`（slider）→ `extractSpeakerNotes` |
| `translateAndGenerateCapsuleVideos` | capsule | 组合：`translateDeck` → `extractSpeakerNotes` → `generateCapsuleVideoAllLanguages` |
| `generateCapsuleContent` | generate | LLM 流水线（koog）：propose-context → 校验 → 演讲者备注 + TTS 脚本 |
| `generateCapsuleContentAndVideos` | capsule | 组合：`generateCapsuleContent` → `translateAndGenerateCapsuleVideos` |
| `collectCapsuleAugmentedContext` | collect | 收集增强上下文（治理 + RAG + Docs + 场景 + 术语表） |
| `capsuleAiSmokeTest` | generate | 以最小提示冒烟测试 codebase LLM 桥 |
| `generateCapsuleTranscript` | generate | 从演讲者备备注生成 AsciiDoc 转写文章 |
| `generateCapsuleChapters` | generate | 章节标记 + YouTube 文本章节 + 描述 |
| `generateCapsulePodcast` | generate | 纯音频播客（拼接的旁白 MP3） |
| `deployCapsule` | deploy | 通过 FFmpeg 将胶囊重新裁切为垂直 9:16（TikTok/Shorts） |
| `distributeCapsuleVideo` | distribute | 将最终视频复制到版本化的目标目录 |
| `collectCapsuleContext` / `transformCapsuleContext` / `collectCapsuleRetrieve` | collect/transform | 导出 / 解析胶囊上下文 JSON（N3 引擎契约） |
| `scaffoldCapsuleContext` | generate | 生成带注释的默认 `capsule-context.yml` |
| `validateCapsuleVideoDuration` | verification | 将视频时长与 TTS 音频时长之和比对（可选） |
| `preflightCapsuleContent` | verification | 付费 TTS 前的内容预检（可选） |
| `renderQcBoards` | verification | 渲染 QC 板（beats +「所说 vs 所示」）（可选） |
| `verifyCapsuleVoice` | verification | 通过 ASR 进行语音 QC——保真度、呼吸、取用选择（可选） |
| `selectBestVoiceTake` | verification | 在幻灯片候选中选择最佳取用（可选） |
| `generateViralCampaign` | generate | Storyboard → 短视频营销活动（YouTube/TikTok）（可选） |
| `renderViralVariants` / `renderViralThumbnails` / `renderViralSubtitles` | generate | 渲染病毒式变体、缩略图、卡拉 OK 字幕（可选） |
| `validateViralCampaign` | verification | 对照实际产出的文件检查清单（可选） |
| `normalizeViralStoryboard` | generate | 补全 storyboard 缺失的转场（创作助手） |

> 可选（*opt-in*）能力**默认禁用**（`false`），绝不阻塞主管道。

## 配置

每个值从 **4 个来源**解析（优先级 **CLI > YAML > gradle.properties > ENV**）：

```bash
./gradlew scaffoldCapsuleContext          # 生成带注释的 capsule-context.yml
./gradlew generateCapsuleVideo -Pcapsule.tts.engine=espeak -Pcapsule.manim.quality=h
```

代表性 DSL（完整 18 节的 `CapsuleConfig` 见[插件内部指南](../capsule-plugin/README.md)）：

```gradle
capsule {
    // 输入
    outputDir.set("capsule")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")

    // TTS
    ttsEngine.set("piper")                 // "piper"（默认）或 "espeak"
    ttsVoice.set("fr_FR-siwis-medium")
    piperExecutablePath.set("piper")
    ttsFallbackEnabled.set(true)
    espeakVoice.set("fr")
    espeakSpeed.set(150)
    ttsLanguage.set("fr")                  // 22 种语言（LanguageCatalog）

    // 捕获 / 输出
    viewportWidth.set(1408)
    viewportHeight.set(792)
    captureStrategy.set("playwright")      // "playwright" | "screenshot" | "remotion"
    outputFormat.set("webm")               // "webm"（默认）| "mp4" | "both"
    outputBt709.set(false)                 // 将 MP4 标记为 BT.709（可选）

    // 字幕
    subtitleEnabled.set(false)
    subtitleFormat.set("srt")              // "srt" 或 "vtt"
    subtitleBurnIn.set(false)

    // 分发
    ffmpegExecutablePath.set("ffmpeg")
    distribOutputWidth.set(1080)
    distribOutputHeight.set(1920)

    // Manim（可选）
    manimExecutablePath.set("manim")
    manimQuality.set("l")                  // l, m, h, p, k
}
```

## 多语言流水线

一个源演示文稿 → 每个目标语言一个本地化胶囊。流水线消费 `slider.translateDeck` 产出的
翻译演示文稿（**22 种语言**，来自 N0 `i18n-contracts` 的 `LanguageCatalog`），并通过纯领域
`capsule.multilang`（`MultiLanguageResolver` + `VoiceMapping`）解析各语言的 TTS 语音。

```
源演示文稿（AsciiDoc）
        ↓ slider.translateDeck（LanguageCatalog，22 种语言）
<deckName>_<lang>-deck.html（每种语言一个）
        ↓ capsule.extractSpeakerNotes
<deckName>_<lang>-script.txt（每种语言一个）
        ↓ capsule.generateCapsuleVideoAllLanguages
<deckName>_<lang>.webm（每种语言一个本地化胶囊）
```

命名约定（边界契约，与 slider 对齐）：

- 翻译演示文稿：`<deckName>_<lang>-deck.html`
- 演讲者脚本：`<deckName>_<lang>-script.txt`
- 本地化视频：`<deckName>_<lang>.webm`

```bash
./gradlew generateCapsuleVideoAllLanguages   # 每个具备演示文稿+脚本对的语言一个视频
./gradlew translateAndGenerateCapsuleVideos  # 完整组合：translate → extract → 视频
```

没有匹配演示文稿 + 脚本对的语言会被静默跳过，并应用**墨水经济**规则：视频已存在且探测
有效的语言绝不重新渲染。

## 可选功能

- **输出格式** — `outputFormat = "mp4"`（或 `"both"`）在原生 WebM 之外产出 MP4/H.264；
  `outputBt709 = true` 将其标记为 BT.709（广播色彩）。
- **音频后期制作** — 背景音乐、EBU R128 响度（2 遍测量）和 sidechain ducking（`audioPost { … }`）。
- **字幕** — SRT/VTT 生成与烧录（`subtitleEnabled`、`subtitleBurnIn`）。
- **质量控制（可选）** — 内容预检（`preflightCapsuleContent`）、通过解码的视频完整性、
  确定性时间线、QC 板（`renderQcBoards`）以及通过 ASR 的语音 QC（`verifyCapsuleVoice`、
  `selectBestVoiceTake`）。
- **Transcript** — 反向流水线：从演讲者备注生成 AsciiDoc 文章（`TEMPLATE` 或 `LLM`）。
- **Podcast** — 纯音频旁白 MP3。
- **Preview** — dry-run（`previewOnly = true`）：仅渲染幻灯片 PNG，跳过合成。
- **病毒式（可选）** — 可校验的 storyboard → 垂直 9:16 短视频活动，hooks/CTA 锚定于增强
  上下文，含缩略图、卡拉 OK 字幕和 `CampaignBundle`。

### 幻灯片节奏（Remotion 捕获）

当 `captureStrategy = "remotion"` 时，演示文稿是动画而非拍摄：每个幻灯片块按阅读节奏出现，
而不是整张幻灯片一次性出现。默认 beats（从幻灯片开始起的秒数）：`.kicker` 0.0 ·
`h1`/`h2` 0.15 · `.rule` 0.5 · `.lede` 1.0 · `.stat` 4.5 · `.statlabel` 5.0。`.brand`、
`.num` 和 `.grid` 属于 chrome（从第一帧即出现）。任何块都可用 `data-at` 设定自己的 beat：

```html
<p class="stat" data-at="8.5">542 076 km</p>
```

## 构建与测试（贡献者）

```bash
./gradlew build                    # 完整构建
./gradlew check                    # 单元 + 功能 + Cucumber
./gradlew test                     # 仅 JUnit5 单元
./gradlew cucumberTest -PrunCucumber
./gradlew functionalTest           # Gradle TestKit
./gradlew publishToMavenLocal      # 本地发布
```

## 故障排除

| 症状 | 解决 |
|------|------|
| 找不到 Playwright Chromium | 运行一次以自动下载，或设置 `chromiumExecutablePath` |
| Piper TTS 无音频 | 检查 `piperExecutablePath`，确保语音模型已安装 |
| `deployCapsule` 失败 | 确认 FFmpeg 在 `PATH` 中（`ffmpeg -version`） |
| Manim 幻灯片未渲染 | 将 `manimExecutablePath` 设为 `noop` 以便无 Manim 测试 |
| 视频捕获超时 | 增大 `playwrightTimeout`（默认 120000 毫秒） |
| 内存不足 | `export GRADLE_OPTS="-Xmx2g"` |

## 文档

- [capsule-plugin/README.md](../capsule-plugin/README.md) — 插件内部（开发者与贡献者指南）
- [capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) — 流水线架构（PlantUML）

## 许可证

Apache License 2.0——见 [LICENSE](../LICENSE)。

---

_CCCP Education 生态系统的一部分——`groupId: education.cccp`。_
