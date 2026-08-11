package capsule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.io.File

/**
 * TDD unit tests for [FormatConversion] pure dispatch (CAP-MP4 US-2).
 *
 * [FormatConversion.convertIfNeeded] dispatches on [OutputFormat]:
 * - WEBM → no-op, returns the WebM file unchanged.
 * - MP4  → transcode via the converter; on success removes the WebM
 *          intermediate and returns the MP4 file.
 * - BOTH → transcode and keep the WebM; returns the WebM (primary).
 *
 * Economy of ink (AGENT.adoc): if the MP4 already exists and has a
 * positive probed duration, the transcode is skipped.
 */
class FormatConversionTest {

    private class FakeConverter(private val succeed: Boolean) : VideoFormatConverter {
        override fun isAvailable(): Boolean = true
        override fun name(): String = "fake-format"
        override fun convertToMp4(webmFile: File, mp4File: File): Boolean {
            if (!succeed) return false
            mp4File.writeText("fake-mp4-content")
            return true
        }
    }

    private fun tmpWebm(): File = File.createTempFile("deck", ".webm").apply { writeText("webm-content") }

    private fun probeDurationZero(@Suppress("UNUSED_PARAMETER") f: File): Double = 0.0
    private fun probeDurationPositive(@Suppress("UNUSED_PARAMETER") f: File): Double = 5.0

    @Test
    fun `WEBM format is a no-op and returns the webm file unchanged`() {
        val webm = tmpWebm()
        try {
            val result = FormatConversion.convertIfNeeded(
                finalVideo = webm,
                format = OutputFormat.WEBM,
                converter = FakeConverter(succeed = true),
                probeDuration = ::probeDurationZero
            )
            assertEquals(webm, result, "WEBM format should return the webm unchanged")
            assertTrue(webm.exists(), "WebM should still exist after WEBM no-op")
        } finally {
            webm.delete()
        }
    }

    @Test
    fun `MP4 format transcodes and removes the webm intermediate on success`() {
        val webm = tmpWebm()
        try {
            val result = FormatConversion.convertIfNeeded(
                finalVideo = webm,
                format = OutputFormat.MP4,
                converter = FakeConverter(succeed = true),
                probeDuration = ::probeDurationZero
            )
            assertTrue(result.name.endsWith(".mp4"), "MP4 format should return the mp4 file")
            assertTrue(result.exists(), "MP4 file should exist")
            assertFalse(webm.exists(), "WebM intermediate should be removed on MP4 success")
        } finally {
            webm.delete()
        }
    }

    @Test
    fun `MP4 format keeps webm when transcode fails`() {
        val webm = tmpWebm()
        try {
            val result = FormatConversion.convertIfNeeded(
                finalVideo = webm,
                format = OutputFormat.MP4,
                converter = FakeConverter(succeed = false),
                probeDuration = ::probeDurationZero
            )
            assertEquals(webm, result, "On transcode failure, return the webm (degraded)")
            assertTrue(webm.exists(), "WebM should still exist when transcode failed")
        } finally {
            webm.delete()
        }
    }

    @Test
    fun `BOTH format transcodes and keeps the webm intermediate`() {
        val webm = tmpWebm()
        try {
            val result = FormatConversion.convertIfNeeded(
                finalVideo = webm,
                format = OutputFormat.BOTH,
                converter = FakeConverter(succeed = true),
                probeDuration = ::probeDurationZero
            )
            assertEquals(webm, result, "BOTH should return the webm (primary)")
            assertTrue(webm.exists(), "WebM should be kept for BOTH")
            val mp4 = File(webm.parentFile, webm.nameWithoutExtension + ".mp4")
            assertTrue(mp4.exists(), "MP4 should also exist for BOTH")
            mp4.delete()
        } finally {
            webm.delete()
        }
    }

    @Test
    fun `MP4 format skips transcode when mp4 already exists and probe is positive`() {
        val webm = tmpWebm()
        val mp4 = File(webm.parentFile, webm.nameWithoutExtension + ".mp4").apply { writeText("existing-mp4") }
        try {
            val result = FormatConversion.convertIfNeeded(
                finalVideo = webm,
                format = OutputFormat.MP4,
                converter = FakeConverter(succeed = true),
                probeDuration = ::probeDurationPositive
            )
            assertTrue(result.name.endsWith(".mp4"), "Should return the mp4")
            assertEquals("existing-mp4", mp4.readText(), "Existing mp4 should NOT be overwritten (economy of ink)")
            assertFalse(webm.exists(), "WebM intermediate should still be removed when reusing existing mp4")
        } finally {
            webm.delete(); mp4.delete()
        }
    }

    @Test
    fun `BOTH format skips transcode when mp4 already exists and probe is positive`() {
        val webm = tmpWebm()
        val mp4 = File(webm.parentFile, webm.nameWithoutExtension + ".mp4").apply { writeText("existing-mp4") }
        try {
            val result = FormatConversion.convertIfNeeded(
                finalVideo = webm,
                format = OutputFormat.BOTH,
                converter = FakeConverter(succeed = true),
                probeDuration = ::probeDurationPositive
            )
            assertEquals(webm, result, "BOTH returns webm primary")
            assertTrue(webm.exists(), "WebM kept for BOTH")
            assertEquals("existing-mp4", mp4.readText(), "Existing mp4 NOT overwritten (economy of ink)")
        } finally {
            webm.delete(); mp4.delete()
        }
    }
}