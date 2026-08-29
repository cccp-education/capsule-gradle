@pr2-cov
Feature: PR #2 coverage regression
  Regression guard for the anti-deadlock / zero-loss-of-data migration (PR #2):
  every external process and every file relocation must route through the single
  sinks ProcessRunner (process) and FileReplace.moveOver (file move). No raw
  ProcessBuilder, no .waitFor, no .renameTo outside the seam.

  Scenario: ProcessRunner is the sole process execution sink
    Given the capsule plugin production source tree
    When the process execution sinks are audited
    Then only "ProcessRunner.kt" and "FileReplace.kt" may contain process or rename APIs

  Scenario: FileReplace moveOver is the sole file relocation sink
    Given the capsule plugin production source tree
    When the file relocation sinks are audited
    Then no file outside "FileReplace.kt" may call renameTo
