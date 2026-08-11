@distrib @format
Feature: Capsule output format distribution (CAP-MP4 US-3)

  As a capsule-gradle producer
  I want to distribute capsule videos in WebM, MP4, or both formats
  So that I can target LMS/YouTube/OF platforms that consume MP4/H.264

  Scenario: Default WEBM format distributes only the WebM file
    Given a capsule format distribution project with a fake WebM "demo.webm" in the build output directory
    When the distributeCapsuleVideo task is run with format "webm"
    Then the format distribution build output contains "CAPSULE DISTRIB"
    And the format distribution copies only video file "demo.webm" to the versioned destination

  Scenario: MP4 format distributes only the MP4 file
    Given a capsule format distribution project with a fake WebM "demo.webm" and a fake MP4 "demo.mp4" in the build output directory
    When the distributeCapsuleVideo task is run with format "mp4"
    Then the format distribution build output contains "CAPSULE DISTRIB"
    And the format distribution copies only video file "demo.mp4" to the versioned destination

  Scenario: BOTH format distributes WebM and MP4 files
    Given a capsule format distribution project with a fake WebM "demo.webm" and a fake MP4 "demo.mp4" in the build output directory
    When the distributeCapsuleVideo task is run with format "both"
    Then the format distribution build output contains "CAPSULE DISTRIB"
    And the format distribution copies video file "demo.webm" to the versioned destination
    And the format distribution copies video file "demo.mp4" to the versioned destination