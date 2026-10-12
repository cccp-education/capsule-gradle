package capsule.scenarios

import capsule.ci.KernelBoundaryAudit
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Pure-domain BDD steps for `capsule_kernel_boundary.feature`
 * (CAP-ENGINE-DECOUPLING US-4).
 *
 * Delegates to [KernelBoundaryAudit]. Step texts are prefixed with
 * "engine-neutral kernel"/"kernel" (S-088 — the glue is classpath-wide).
 */
class CapsuleKernelBoundarySteps {

    private var violations: List<String> = emptyList()

    @Given("the engine-neutral kernel source files")
    fun theEngineNeutralKernelSourceFiles() {
        assertTrue(KernelBoundaryAudit.files().isNotEmpty(), "kernel source set must not be empty")
    }

    @When("their import statements are audited")
    fun theirImportStatementsAreAudited() {
        violations = KernelBoundaryAudit.importViolations()
    }

    @When("their code is audited for renderer vocabulary")
    fun theirCodeIsAuditedForRendererVocabulary() {
        violations = KernelBoundaryAudit.vocabularyViolations()
    }

    @Then("no kernel import references a forbidden renderer or build-system package")
    fun noKernelImportReferencesAForbiddenPackage() {
        assertTrue(violations.isEmpty(), "forbidden kernel imports: $violations")
    }

    @Then("no kernel code statement mentions a renderer or build-system token")
    fun noKernelCodeStatementMentionsARendererToken() {
        assertTrue(violations.isEmpty(), "forbidden kernel vocabulary: $violations")
    }
}
