package capsule.ci

import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * OSS vocabulary guard (RENAME S-153).
 *
 * capsule is a public Apache-2.0 brique published to the Gradle Plugin Portal.
 * Its self-description must stay generic ("video capsule"): the FPA/formation
 * metier vocabulary (FPA, formation, formateur, pédagogique) belongs to the
 * training borough, never here.
 *
 * The RENAME-4 pass (S-135) neutralised FPA/AFNOR/REAC/SPD/SPG but the later
 * CAP-VIRAL / CAP-CONTEXT work reintroduced FPA and formation in the fixtures,
 * and the "capsule pédagogique" self-description had never been neutralised.
 * This guard locks the whole `src/` tree — production, resources (shipped in
 * the jar), fixtures, features and docs — so the residue cannot reappear
 * silently. English generic vocabulary ("pedagogical", "training") is allowed;
 * only the FPA/formation metier terms are forbidden.
 */
class OssVocabularyGuardTest {

    /** The guard's own documentation names the forbidden terms — it is exempt. */
    private val selfName = "OssVocabularyGuardTest.kt"

    /**
     * FPA/formation metier vocabulary. Word boundaries keep French "information"
     * / "transformation" out; the English "pedagogical" is not matched.
     */
    private val metierPattern = Regex("""(?i)\b(fpa|formation|formateur)\b|p[ée]dagogique""")

    @Test
    fun `plugin POM description keeps the generic capsule wording`() {
        val build = File("build.gradle.kts").readText()
        val description = Regex("""description\.set\("(.+?)"\)""")
            .find(build)?.groupValues?.get(1)
            ?: error("description.set(\"...\") not found in build.gradle.kts")
        assertThat(description).containsIgnoringCase("capsule")
        assertThat(metierPattern.find(description)?.value)
            .describedAs("the POM description is the OSS self-description shipped in the jar")
            .isNull()
    }

    @Test
    fun `no source file carries FPA or formation metier vocabulary`() {
        val root = File("src")
        require(root.isDirectory) { "source root not found at ${root.absolutePath}" }
        val violations = root.walkTopDown()
            .filter { it.isFile && it.name != selfName }
            .mapNotNull { file ->
                metierPattern.find(file.readText())?.let { "${file.relativeTo(root)}: '${it.value}'" }
            }
            .toList()
        assertThat(violations)
            .describedAs("the FPA/formation metier vocabulary belongs to the training borough, not to the OSS brique")
            .isEmpty()
    }
}
