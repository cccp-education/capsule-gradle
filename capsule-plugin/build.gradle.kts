plugins {
    id("education.cccp.build.gradle-plugin") version "0.0.1"
    id("education.cccp.build.publishing") version "0.0.1"
    id("education.cccp.build.functional-test") version "0.0.1"
    id("education.cccp.build.cucumber") version "0.0.1"
    alias(libs.plugins.kover)
}

group = "education.cccp"
version = libs.plugins.capsule.get().version

repositories {
    mavenLocal()
}

cucumberConventions {
    featuresDir = "src/test/features"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    compileOnly(libs.slider)
    implementation(libs.playwright)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.koog.agents)

    testImplementation(kotlin("test-junit5"))
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
