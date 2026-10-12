@kernel-boundary
Feature: Kernel boundary
  The engine-neutral kernel (capsule.kernel) must stay free of any renderer
  (reveal.js, Playwright, AsciiDoc) or build-system (Gradle) vocabulary: it
  speaks segment, visual and engine only, so a second engine can implement the
  ports without dragging capsule's reveal coupling along.

  Scenario: the kernel imports no renderer or build-system package
    Given the engine-neutral kernel source files
    When their import statements are audited
    Then no kernel import references a forbidden renderer or build-system package

  Scenario: the kernel code carries no renderer vocabulary
    Given the engine-neutral kernel source files
    When their code is audited for renderer vocabulary
    Then no kernel code statement mentions a renderer or build-system token
