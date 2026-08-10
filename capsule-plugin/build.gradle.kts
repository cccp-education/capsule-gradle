// ── buildscript resolutionStrategy ────────────────────────────────────────────────
// koog-agents 1.0.0 → koog-utils/koog-http-client-core/koog-prompt-llm → annotations.
// codebase-plugin exclut koog-agents mais les sous-modules koog transitifs
// contournent l'exclusion. Solution : forcer annotations:26.0.2-1 (pattern slider).
import build.CucumberTaskSpec
buildscript {
    repositories { mavenLocal(); mavenCentral() }
    configurations.all { resolutionStrategy { force("org.jetbrains:annotations:26.0.2-1") } }
}

plugins {
    id("education.cccp.build.gradle-plugin") version "0.0.2"
    id("education.cccp.build.publishing") version "0.0.2"
    id("education.cccp.build.functional-test") version "0.0.2"
    id("education.cccp.build.cucumber") version "0.0.2"
    alias(libs.plugins.kover)
    alias(libs.plugins.codebase)
}

group = "education.cccp"
version = libs.plugins.capsule.get().version

repositories {
    mavenLocal()
}

cucumberConventions {
    featuresDir = "src/test/features"
    additionalTasks = listOf(
        // Focused cucumber run for the CAP-ARCH-2 augmented context feature
        // (fast iteration without the full Playwright suite, ~15 min).
        CucumberTaskSpec(
            name = "cucumberTestContext",
            features = listOf("src/test/features/capsule_context.feature"),
            tags = listOf("@context"),
            runnerClass = "capsule.scenarios.CapsuleContextCucumberRunner",
            timeoutMinutes = 30,
        ),
        // Focused cucumber run for the CAP-ARCH-4 content+videos wiring feature
        // (fast iteration without the full Playwright suite, ~15 min).
        CucumberTaskSpec(
            name = "cucumberTestContentAndVideos",
            features = listOf("src/test/features/capsule_content_and_videos.feature"),
            tags = listOf("@architecture"),
            runnerClass = "capsule.scenarios.CapsuleContentAndVideosCucumberRunner",
            timeoutMinutes = 30,
        ),
        // Focused cucumber run for the CAP-ARCH-7 US-4 video destination versioning feature
        // (fast iteration without the full Playwright suite, ~15 min).
        CucumberTaskSpec(
            name = "cucumberTestDistrib",
            features = listOf("src/test/features/capsule_distrib.feature"),
            tags = listOf("@distrib"),
            runnerClass = "capsule.scenarios.CapsuleDistribCucumberRunner",
            timeoutMinutes = 30,
        ),
        // Integration cucumber run — scenarios tagged @integration or @manim
        // (burn-in E2E with real ffmpeg, Manim pipeline NoOp). Excluded from the
        // default cucumberTest runner by `not @integration`; this dedicated task
        // exercises them via CucumberIntegrationTestRunner. Run explicitly:
        //   ./gradlew cucumberTestIntegration -PrunCucumber
        CucumberTaskSpec(
            name = "cucumberTestIntegration",
            features = listOf("src/test/features"),
            tags = listOf("@integration"),
            runnerClass = "capsule.scenarios.CucumberIntegrationTestRunner",
            timeoutMinutes = 30,
        ),
    )
}

// CR-10 — cucumberTest is expensive (Playwright + FFmpeg, ~15 min).
// Skip unless -PrunCucumber or CI env var is active.
// Decision logic documented/tested in capsule.ci.CucumberTestGuard (pure domain).
afterEvaluate {
    val hasRunCucumber = project.hasProperty("runCucumber")
    val isCi = System.getenv("CI") == "true"
    val shouldRun = hasRunCucumber || isCi

    tasks.named("cucumberTest").configure {
        onlyIf { shouldRun }
        doFirst {
            if (!shouldRun) {
                logger.lifecycle("cucumberTest skipped (pass -PrunCucumber or set CI=true to enable)")
            }
        }
    }
}

dependencies {
    implementation(platform("education.cccp:workspace-bom:${libs.versions.workspace.bom.get()}"))
    implementation(kotlin("stdlib-jdk8"))

    compileOnly(libs.slider)
    implementation(libs.playwright)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.dataformat.yaml)

    // N1 codebase — LLM socle (CAP-ARCH-1): LlmBuildService + LlmProvider
    implementation(libs.codebase.plugin)

    // koog-agents — orchestration DSL (CAP-ARCH-3). Not transitive via codebase
    // (codebase exposes it as `implementation`), so capsule depends directly via BOM.
    implementation(libs.koog.agents) {
        exclude(group = "org.jetbrains", module = "annotations")
    }

    // langchain4j — ChatModel bridge (LlmProviderChatModelAdapter, CAP-ARCH-1)
    implementation(libs.langchain4j)
    implementation(libs.langchain4j.ollama)

    // N0 contracts — i18n (LanguageCatalog 10 languages, cross-borough translation alliance)
    implementation(libs.i18n.contracts)

    // N0 contracts — codebase context (CompositeContext/ContextChannel/ChannelBudget, CAP-ARCH-2)
    implementation(libs.codebase.contracts)

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.assertj:assertj-core:3.27.7")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.slf4j:slf4j-api:2.0.17")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.26")
    testImplementation(libs.bundles.cucumber)
}

afterEvaluate {
    configurations.getByName("functionalTestImplementation").extendsFrom(
        configurations.getByName("testImplementation")
    )
}

gradlePlugin {
    val capsule by plugins.creating {
        id = "education.cccp.capsule"
        implementationClass = "capsule.CapsulePlugin"
    }
}

kover {
    reports {
        total {
            xml { onCheck = true }
            html { onCheck = true }
        }
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            if (name == "pluginMaven") {
                pom {
                    name.set("Capsule Gradle Plugin")
                    description.set("Generation automatisee de capsules video pedagogiques depuis des decks reveal.js")
                    url.set("https://github.com/cheroliv/capsule-gradle/")
                }
            }
        }
    }
}