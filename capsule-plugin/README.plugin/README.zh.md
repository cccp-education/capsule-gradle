<!-- translated from README.md rev 0.0.1-SNAPSHOT -->
# capsule-gradle — 插件内部机制

> `capsule-plugin` Gradle 插件的开发者与贡献者指南。

[![Maven Central](https://img.shields.io/static/v1?label=Maven%20Central&message=0.0.1-SNAPSHOT&color=orange)](https://central.sonatype.com/artifact/education.cccp/capsule-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/education.cccp.capsule.svg?label=Plugin%20Portal)](https://plugins.gradle.org/plugin/education.cccp.capsule)
[![CI](https://img.shields.io/github/actions/workflow/status/cheroliv/capsule-gradle/test.yml?branch=main&label=CI)](https://github.com/cheroliv/capsule-gradle/actions/workflows/test.yml)
[![Coverage](https://img.shields.io/static/v1?label=coverage&message=Kover&color=blue)]()
[![License](https://img.shields.io/github/license/cheroliv/capsule-gradle?label=License)](../LICENSE)

- **版本**：`0.0.1-SNAPSHOT`（未发布）· **组**：`education.cccp` · **插件 ID**：`education.cccp.capsule`
- **工具链**：Java 24 · Kotlin 2.3.20 · Gradle 9.5
- **构建**：`./gradlew build -x test` · **测试**：`./gradlew check`（195 单元 + 22 Cucumber 通过）· **覆盖率**：`./gradlew koverReport`

🌐 Languages: [English](README.md) | **中文** | [हिन्दी](README.hi.md) | [Español](README.es.md) | [Français](README.fr.md) | [العربية](README.ar.md) | [বাংলা](README.bn.md) | [Português](README.pt.md) | [Русский](README.ru.md) | [اردو](README.ur.md)

---

## 模块布局

```
capsule-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml          # 版本目录
├── doc/
│   └── CAPSULE_ARCHITECTURE.adoc      # 流水线架构（PlantUML）
└── src/
    ├── main/kotlin/capsule/
    │   ├── CapsulePlugin.kt           # 入口点 —— 应用 slider、注册任务、配置合并
    │   ├── CapsuleManager.kt          # 任务注册 + 脚本解析 + 工厂方法
    │   ├── CapsuleModels.kt           # CapsuleExtension DSL + CapsuleConventions + 数据模型
    │   ├── CapsuleConfig.kt           # 不可变配置（5 个部分：input, tts, capture, distrib, manim）
    │   ├── CapsuleConfigLoader.kt     # YAML 加载器，支持 ${VAR} 环境变量解析
    │   ├── CapsuleConfigMerger.kt     # 4 来源合并（CLI > YAML > props > ENV）
    │   ├── CapsuleScriptTask.kt       # generateCapsuleScript
    │   ├── CapsuleBuildTask.kt        # generateCapsule（TTS 合成）
    │   ├── CapsuleVideoTask.kt        # generateCapsuleVideo（音频注入 + Playwright 捕获）
    │   ├── CapsuleDistribTask.kt     # deployCapsule（9:16 FFmpeg 裁剪）
    │   ├── CapsuleCompositeContextTask.kt  # collectCapsuleContext（N3 JSON 导出）
    │   ├── CapsuleParseContextTask.kt # transformCapsuleContext + collectCapsuleRetrieve
    │   ├── CapsuleScaffoldTask.kt    # scaffoldCapsuleContext
    │   ├── TtsManager.kt             # Piper + espeak TTS 集成
    │   ├── PlaywrightManager.kt      # 无头 Chromium 捕获
    │   ├── ManimEngine.kt            # Manim 渲染（NoOp/Impl）
    │   ├── ManimSlideReplacer.kt     # HTML→视频嵌入替换
    │   ├── ManimVideoMixer.kt        # 复用 Manim MP4 + TTS 音频
    │   ├── ManimParallelRenderer.kt  # 并行 Manim 渲染
    │   ├── SubtitleModels.kt         # 字幕数据模型
    │   ├── SubtitleTimingCalculator.kt
    │   ├── SrtGenerator.kt           # SRT 字幕生成
    │   ├── VttGenerator.kt           # VTT 字幕生成
    │   ├── SubtitleBurnInService.kt  # FFmpeg 字幕烧录
    │   ├── HtmlSectionParser.kt       # reveal.js 演示文稿 HTML 解析
    │   ├── AudioConversionUtil.kt    # WAV→MP3 转换
    │   ├── MediaProbeUtil.kt         # ffprobe 时长探测
    │   └── LanguageVoiceMapping.kt    # FR/EN/ES/DE 语音映射
    ├── test/kotlin/capsule/
    │   ├── CapsulePluginTest.kt
    │   ├── scenarios/                 # Cucumber BDD 步骤定义 + 运行器
    │   │   ├── CapsuleStepDefinitions.kt
    │   │   ├── CucumberTestRunner.kt
    │   │   └── CucumberIntegrationTestRunner.kt
    │   └── *Test.kt                   # 17 个单元测试类
    ├── test/features/
    │   └── capsule.feature            # 22 个 Cucumber 场景
    └── functionalTest/kotlin/capsule/
        └── CapsulePluginFunctionalTest.kt
```

## N2 依赖

`capsule-gradle` 通过 `compileOnly` 消费 `slider-gradle`（`education.cccp:slider:0.0.6`）。
契约是基于文件的——capsule 读取 slider 生成的演示文稿 HTML 和脚本文本文件，
从不修改它们。CapsulePlugin 在运行时若可用则自动应用 `education.cccp.slider`。

## 关键库

| 库 | 版本 | 角色 |
|---------|---------|------|
| **Playwright** | 1.52.0 | 无头 Chromium —— reveal.js 控制 + 视频捕获（JVM 原生，零 npm） |
| **koog-agents** | 0.8.0 | 代理编排的 Kotlin DSL（StateGraph / ConditionalEdges） |
| **Jackson** | 2.18.3 | YAML 配置 + JSON 序列化（databind, kotlin, dataformat-yaml） |
| **Kover** | 0.9.8 | 覆盖率报告（XML + HTML，接入 `check`） |
| **Cucumber** | 7.34.3 | BDD 测试（cucumber-java, junit-platform-engine, picocontainer） |
| **Kotlin** | 2.3.20 | 插件 DSL |
| **slider** | 0.0.6 | compileOnly —— reveal.js 演示文稿 + 胶囊脚本来源 |

外部工具（非 Maven 依赖）：
- **Piper** —— 本地离线 TTS 引擎（默认）
- **espeak** —— TTS 备用引擎
- **FFmpeg** —— 9:16 裁剪（`deployCapsule`）和字幕烧录
- **Manim** —— 数学动画渲染（可选，脚本中以 `[manim:SceneName]` 标记）
- **Chromium** —— 首次运行时由 Playwright 自动下载

## 测试矩阵

| 任务 | 范围 | 详情 |
|------|-------|---------|
| `test` | JUnit5 单元测试 | 排除 `@integration` 标签和 `capsule.scenarios.**` |
| `cucumberTest` | Cucumber BDD | 22 个场景，`forkEvery = 1`，`maxParallelForks = 1`，堆 1g |
| `functionalTest` | Gradle TestKit | 插件应用功能测试 |
| `check` | 全部 | 依赖 `test` + `cucumberTest` + `functionalTest` |

Cucumber 标签：`@integration`、`@manim`、`@config`、`@tts`、`@subtitles`、`@burnin`、`@style`、`@parallel`。

测试总计：**195 单元 + 22 Cucumber = 217 通过**（会话 029 基线）。

## JVM 调优

Cucumber 测试使用 `maxHeapSize = "1g"`，配合 `forkEvery = 1`（每个场景一个全新 JVM）
和 `maxParallelForks = 1`（顺序执行以保证稳定性）。

对于本地构建：
```bash
export GRADLE_OPTS="-Xmx2g"
```

## 构建命令

```bash
./gradlew build                       # 完整构建（编译 + 测试）
./gradlew build -x test               # 仅编译
./gradlew check                       # 所有测试（单元 + Cucumber + 功能）
./gradlew test                        # 仅 JUnit5 单元
./gradlew cucumberTest                # Cucumber BDD
./gradlew functionalTest              # Gradle TestKit
./gradlew koverReport                 # 覆盖率报告（XML + HTML）
./gradlew publishToMavenLocal         # 本地发布
```

## CI 流水线

`.github/workflows/test.yml` 定义一个作业：
1. **Build & Test** —— 每次推送到 main/master 或提交 PR 时运行 `./gradlew check`（≤ 15 分钟，JDK 24 Temurin，Gradle setup action）

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

## 发布（NMCP）

**未发布** —— 版本 `0.0.1-SNAPSHOT`。POM 通过 `maven-publish` 和 `signing` 插件为 Maven Central 配置：

- POM：名称 "Capsule Gradle Plugin"，描述 "Generation automatisee de capsules video
  pedagogiques depuis des decks reveal.js"，URL `github.com/cheroliv/capsule-gradle/`
- 签名：`useGpgCmd()`（已配置但尚未执行）
- 发布目标：`publishToMavenLocal` 可用；NMCP `publishAggregationToCentralPortal`
  需在 `~/.gradle/gradle.properties` 中提供凭证

准备就绪后发布：
```bash
./gradlew publishAggregationToCentralPortal --no-daemon
```

## 架构文档

- [CAPSULE_ARCHITECTURE.adoc](../capsule-plugin/doc/CAPSULE_ARCHITECTURE.adoc) —— 流水线
  架构，含 PlantUML 图（全局流水线、组件、序列、部署）
- [.agents/INDEX.adoc](../capsule-plugin/.agents/INDEX.adoc) —— EPIC、路线图与治理
- [AGENT.adoc](../capsule-plugin/AGENT.adoc) —— 绝对规则（7 条规则，墨水经济法则）
- [CODE_REVIEW.adoc](../capsule-plugin/CODE_REVIEW.adoc) —— 代码审查（10 个可操作 EPIC CR-1→CR-10）

## EPIC 状态

CAP-0 到 CAP-25 已完成。活跃 EPIC（见 `.agents/INDEX.adoc`）：

| EPIC | 描述 | 状态 |
|------|-------------|--------|
| CR-1 | 重构 `CapsuleVideoTask.execute()`（218→76 行） | ✅ 已完成（会话 040） |
| CR-2 | 线程安全 + `captureSlideParallel` 超时 | ⬜ 待办 |
| CR-3 | TTS 错误处理 —— 降级信号 | ⬜ 待办 |
| CR-4 | HTML 转义安全 —— `injectSubtitleTrack` | ⬜ 待办 |
| CR-5 | DRY `CapsuleConfigMerger`（约 76 行重复） | ⬜ 待办 |
| CAP-27 | VTT 烧录（目前仅 SRT） | ⬜ 待办 |
| CAP-28 | 覆盖率缺口 —— PiperTtsEngine 31%，ManimEngineImpl 25% | ⬜ 待办 |

## 贡献

1. 构建可编译：`./gradlew build -x test`
2. 所有测试通过：`./gradlew check`
3. 遵循 7 条绝对规则（见 [AGENT.adoc](../capsule-plugin/AGENT.adoc)）：
   - 未经许可不提交/推送
   - DAG N2 —— 可被 N3 导入，从不导入 N3
   - Capsule 消费 slider（只读，从不修改源演示文稿）
   - 代码中零密钥/令牌
4. 遵守墨水经济法则 —— 当同一输入的有效工件已存在时，绝不重新执行昂贵操作（TTS、Playwright 捕获）

## 许可证

Apache License 2.0 —— 见 [LICENSE](../LICENSE)。

---

_CCCP Education 生态系统的一部分 —— `groupId: education.cccp`。_