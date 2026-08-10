package capsule.context

import contracts.context.ChannelBudget
import contracts.context.ContextChannel
import java.io.File

/**
 * Pure loader for the documentary corpus that feeds the Docs channel of
 * [CompositeContext] (CAP-DOCCONTEXT-2).
 *
 * This object resolves pre-matched files (the glob → file resolution happens
 * in the Gradle wiring layer via `project.fileTree()`), reads each file,
 * concatenates them sorted by name with `--- filename ---` headers, and
 * truncates the result to the Docs channel token budget
 * ([ChannelBudget.docsTokens] = 10% of the total budget by default).
 *
 * The truncation reuses the N0 [ContextChannel.Docs.truncateToTokens] method
 * so the budget semantics are identical to the contract's. A zero-docs-budget
 * yields a blank string (no content), which the builder will drop as an empty
 * channel — backward compatible with the pre-DOCCONTEXT behavior.
 *
 * Missing files are skipped silently — a glob that matches nothing produces
 * no error, only an empty channel.
 */
object DocContextLoader {

    /**
     * Loads the documentary content from [files], truncated to the Docs budget.
     *
     * @param files  pre-resolved files (glob resolution done by the wiring layer)
     * @param budget the N0 token budget — [ChannelBudget.docsTokens] drives truncation
     * @return the concatenated `--- filename ---\n<content>` block, truncated
     *         to `budget.docsTokens`. Blank when no files or zero budget.
     */
    fun load(files: List<File>, budget: ChannelBudget): String {
        val maxTokens = budget.docsTokens
        if (maxTokens <= 0 || files.isEmpty()) return ""

        val sorted = files
            .filter { it.exists() && it.readText().isNotBlank() }
            .sortedBy { it.name }

        if (sorted.isEmpty()) return ""

        val raw = sorted.joinToString("\n\n") { file ->
            "--- ${file.name} ---\n${file.readText().trim()}"
        }

        val truncated = ContextChannel.Docs(raw).truncateToTokens(maxTokens)
        return truncated.content
    }
}