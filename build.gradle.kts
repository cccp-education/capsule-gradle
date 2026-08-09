buildscript {
    repositories { mavenLocal(); mavenCentral() }
    configurations.all {
        resolutionStrategy.force("education.cccp:capsule-plugin:${libs.plugins.capsule.get().version}")
    }
}

plugins {
    alias(libs.plugins.slider)
    alias(libs.plugins.capsule)
}
//opencode -s ses_1cfe069beffeleql0GVIXgr5Np
repositories {
    mavenLocal()
    mavenCentral()
}

capsule {
    outputDir.set("capsules")
    sliderScriptDir.set("capsule")
    deckSourceDir.set("docs/asciidocRevealJs")
    ttsEngine.set("espeak")
}

