<!-- translated from README.md rev 0.0.1-SNAPSHOT -->
# capsule-gradle — 用户指南

> Gradle 插件，从 reveal.js 演示文稿自动生成教学视频胶囊。

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.1-SNAPSHOT&color=orange)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![License](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=License)](../LICENSE)

- **版本**：`0.0.1-SNAPSHOT`（未发布）· **组**：`education.cccp` · **插件 ID**：`education.cccp.capsule`
- **构建**：`./gradlew build` · **测试**：`./gradlew check`（195 单元 + 22 Cucumber 通过）
- **覆盖率**：Kover XML + HTML 报告已接入 `check`

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## 功能介绍

`capsule-gradle` 将由 `slider-gradle` 生成的 reveal.js 演示文稿转换为带有同步 TTS 旁白的教学视频胶囊——无需后期制作。

该流水线编排四个外部工具：

```
AsciiDoc source → slider-gradle → reveal.js deck + capsule script
                                      ↓
              capsule-gradle → TTS (Piper/espeak) → audio injection → Playwright capture → WebM video
                                      ↓                              ↓
                              Manim animation (optional)      FFmpeg distrib (9:16)
```

CCCP Education 多插件生态系统的一部分（`groupId: education.cccp`）。

## 快速开始

### 1. 应用插件

```gradle
plugins {
    id("education.cccp.capsule") version "0.0.1-SNAPSHOT"
}
```

### 2. 配置胶囊

```gradle
capsule {
    outputDir.set("capsules")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    ttsEngine.set("espeak")
}
```

### 3. 生成视频

```bash
./gradlew generateCapsuleVideo
```

这会运行完整流水线：脚本校验 → TTS 合成 → 音频注入到演示文稿 HTML → Playwright 无头捕获 → WebM 输出。

### 与 slider 的完整流水线

根 `build.gradle.kts` 将 slider 与 capsule 连接在一起：

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
./gradlew capsulevideo    # slider 演示文稿 + 脚本 → 胶囊视频
```

## 可用任务

| 任务 | 组 | 说明 |
|------|------|-------------|
| `generateCapsuleScript`   | generate  | 从 slider 读取 `*-script.txt` 并校验胶囊脚本 |
| `generateCapsule`          | generate  | 从胶囊脚本生成 TTS 音频文件（Piper/espeak） |
| `generateCapsuleVideo`     | generate  | 将 TTS 音频注入演示文稿 HTML，随后通过 Playwright 捕获视频 |
| `deployCapsule`            | deploy    | 通过 FFmpeg 将胶囊裁剪为竖屏 9:16（TikTok/Shorts） |
| `collectCapsuleContext`    | collect   | 将胶囊上下文（视频路径 + 元数据）导出为 JSON，供 N3 引擎使用 |
| `transformCapsuleContext`  | transform | 解析 `capsule-context.json` 并返回演示文稿列表 |
| `collectCapsuleRetrieve`   | collect   | 从上下文 JSON 检索胶囊演示文稿（N3 引擎契约） |
| `scaffoldCapsuleContext`   | generate  | 生成带注释的默认 `capsule-context.yml` 配置文件 |

## 扩展 DSL

```gradle
capsule {
    // 输入
    outputDir.set("capsule")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    chromiumExecutablePath.set("")

    // TTS
    ttsEngine.set("piper")                    // "piper" 或 "espeak"
    ttsVoice.set("fr_FR-siwis-medium")
    piperExecutablePath.set("piper")
    ttsFallbackEnabled.set(true)
    espeakVoice.set("fr")
    espeakSpeed.set(150)
    ttsLanguage.set("fr")                     // fr, en, es, de

    // 捕获
    viewportWidth.set(1408)
    viewportHeight.set(792)
    playwrightTimeout.set(120_000.0)
    slideDurationSeconds.set(5.0)
    parallelCaptureEnabled.set(false)
    parallelCaptureThreads.set(4)

    // 字幕
    subtitleEnabled.set(false)
    subtitleFormat.set("srt")                 // "srt" 或 "vtt"
    subtitleBurnIn.set(false)
    subtitleBurnInFontSize.set(24)
    subtitleBurnInFontColor.set("&H00FFFFFF")
    subtitleBurnInOutlineColor.set("&H00000000")
    subtitleBurnInPosition.set("bottom")      // "bottom" 或 "top"

    // 分发
    ffmpegExecutablePath.set("ffmpeg")
    distribOutputWidth.set(1080)
    distribOutputHeight.set(1920)

    // Manim 动画
    manimExecutablePath.set("manim")
    manimQuality.set("l")                     // l, m, h, p, k
    manimScriptsDir.set("src/manim")
    manimOutputDir.set("build/capsule/manim")
    manimParallelRender.set(false)
    manimParallelRenderThreads.set(4)
}
```

### 多来源配置

配置从 4 个来源解析（优先级：CLI > YAML > gradle.properties > ENV）：

```bash
./gradlew scaffoldCapsuleContext          # 生成 capsule-context.yml
./gradlew generateCapsuleVideo -Pcapsule.tts.engine=espeak -Pcapsule.manim.quality=h
```

## 前置条件

- **Java** 24+（Kotlin 2.3.20 工具链）
- **Gradle** 9.5+
- **Piper**（TTS，本地离线）—— 默认引擎
- **espeak**（TTS 备用引擎）
- **Chromium**（首次运行时由 Playwright 自动下载）
- **FFmpeg**（用于 `deployCapsule` 9:16 裁剪和字幕烧录）
- **Manim**（可选，用于数学动画幻灯片）

## 构建与测试

```bash
./gradlew build                    # 完整构建
./gradlew check                    # 单元 + 功能 + Cucumber 测试
./gradlew test                     # JUnit5 单元测试（排除 @integration）
./gradlew cucumberTest             # Cucumber BDD 测试
./gradlew functionalTest           # Gradle TestKit 功能测试
./gradlew publishToMavenLocal      # 本地发布
```

## 故障排查

| 症状 | 解决办法 |
|---------|-----|
| 未找到 Playwright Chromium     | 运行一次以自动下载，或设置 `chromiumExecutablePath` |
| Piper TTS 未生成音频       | 检查 `piperExecutablePath`，确保已安装语音模型 |
| `deployCapsule` 失败              | 验证 FFmpeg 是否在 PATH 中（`ffmpeg -version`） |
| Manim 幻灯片未渲染          | 将 `manimExecutablePath` 设为 `noop` 以便在无 Manim 环境下测试 |
| 视频捕获超时              | 增大 `playwrightTimeout`（默认 120000ms） |
| 内存不足                      | `export GRADLE_OPTS="-Xmx2g"` |

更多流水线细节见 [CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc)。

## 许可证

Apache License 2.0 —— 见 [LICENSE](../LICENSE)。

---

_CCCP Education 生态系统的一部分 —— `groupId: education.cccp`。_