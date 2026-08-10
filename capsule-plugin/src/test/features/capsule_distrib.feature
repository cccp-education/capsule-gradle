@distrib @versioning
Feature: Distribute capsule video to versioned destination (CAP-ARCH-7 US-4)

  As a capsule-gradle producer
  I want to copy final WebM videos to a versioned destination directory
  So that I can compare capsule versions side-by-side

  Scenario: The distributeCapsuleVideo task is registered in the distribute group
    Given a capsule distrib project with a fake WebM "demo.webm" in the build output directory
    When the distrib tasks are listed in the "distribute" group
    Then the distrib task list contains "distributeCapsuleVideo"

  Scenario: A WebM is copied to a timestamped versioned destination
    Given a capsule distrib project with a fake WebM "demo.webm" in the build output directory
    When the distributeCapsuleVideo task is run with versioning "timestamp"
    Then the distrib build output contains "CAPSULE DISTRIB"
    And a versioned directory matching prefix "v" is created under the destination root
    And the video file "demo.webm" is copied to the versioned destination

  Scenario: A WebM is copied to an incremental versioned destination
    Given a capsule distrib project with a fake WebM "demo.webm" in the build output directory
    When the distributeCapsuleVideo task is run with versioning "incremental"
    Then the distrib build output contains "CAPSULE DISTRIB"
    And a versioned directory named "v1" is created under the destination root
    And the video file "demo.webm" is copied to the versioned destination

  Scenario: A second incremental run creates version v2
    Given a capsule distrib project with a fake WebM "demo.webm" in the build output directory
    And an existing versioned directory "v1" under the destination root
    When the distributeCapsuleVideo task is run with versioning "incremental"
    Then the distrib build output contains "CAPSULE DISTRIB"
    And a versioned directory named "v2" is created under the destination root

  Scenario: Re-running the distribution skips existing valid copies (economy of ink)
    Given a capsule distrib project with a fake WebM "demo.webm" in the build output directory
    When the distributeCapsuleVideo task is run with versioning "incremental"
    And the distributeCapsuleVideo task is run again with the same arguments
    Then the distrib build output contains "skipped"