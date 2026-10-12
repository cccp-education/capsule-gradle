package capsule.ci

import java.io.File

/**
 * Static audit of the engine-neutral kernel boundary (CAP-ENGINE-DECOUPLING US-4).
 *
 * Shared by the unit guard `KernelBoundaryTest` and the BDD
 * `capsule_kernel_boundary.feature` steps: it reads the `capsule.kernel` source
 * set and reports any violation of the neutral contract.
 *
 * Two rules:
 * 1. **Imports** — the kernel may only depend on `java.`, `kotlin.`, the pure
 *    N0 contracts (`contracts.`) or itself (`capsule.kernel.`). No Gradle, no
 *    Playwright, no adapter, no legacy `capsule.feed`.
 * 2. **Code vocabulary** — after stripping comments, the kernel code must not
 *    mention a renderer or build-system token (`slide`, `deck`, `reveal`,
 *    `playwright`, `data-audio`, `data-capsule-slide`, `asciidoc`,
 *    `processbuilder`, `org.gradle`).
 */
object KernelBoundaryAudit {

    private val kernelDir = File("src/main/kotlin/capsule/kernel")

    internal val allowedImportPrefixes = listOf("java.", "kotlin.", "contracts.", "capsule.kernel.")

    internal val forbiddenTokens = listOf(
        "playwright",
        "reveal",
        ".slides",
        "data-audio",
        "data-capsule-slide",
        "asciidoc",
        "processbuilder",
        "org.gradle",
        "slide",
        "deck",
    )

    fun files(): List<File> {
        require(kernelDir.isDirectory) { "kernel source dir not found at ${kernelDir.absolutePath}" }
        return kernelDir.walkTopDown().filter { it.isFile && it.name.endsWith(".kt") }.toList()
    }

    /** Removes block and line comments so the vocabulary rule only sees code. */
    fun stripComments(source: String): String =
        source.replace(Regex("(?s)/\\*.*?\\*/"), " ")
            .lines().joinToString("\n") { it.substringBefore("//") }

    /** Import statements referencing a forbidden package, as `file: import …`. */
    fun importViolations(): List<String> =
        files().flatMap { file ->
            file.readLines().map { it.trim() }
                .filter { it.startsWith("import ") }
                .mapNotNull { line ->
                    val fqcn = line.removePrefix("import ").removeSuffix(";").trim()
                    if (allowedImportPrefixes.any { fqcn.startsWith(it) }) null else "${file.name}: $line"
                }
        }

    /** Renderer/build tokens found in kernel code (comments excluded). */
    fun vocabularyViolations(): List<String> =
        files().flatMap { file ->
            val code = stripComments(file.readText()).lowercase()
            forbiddenTokens.filter { code.contains(it) }.map { "${file.name}: '$it'" }
        }
}
