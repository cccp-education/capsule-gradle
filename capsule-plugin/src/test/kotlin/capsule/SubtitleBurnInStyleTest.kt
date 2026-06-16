package capsule

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SubtitleBurnInStyleTest {

    @Test
    fun `default style has fontSize 24 white color and bottom position`() {
        val style = SubtitleBurnInStyle()
        assertEquals(24, style.fontSize)
        assertEquals("&H00FFFFFF", style.fontColor)
        assertEquals("&H00000000", style.outlineColor)
        assertEquals("bottom", style.position)
    }

    @Test
    fun `custom style accepts valid values`() {
        val style = SubtitleBurnInStyle(
            fontSize = 36,
            fontColor = "&H000000FF",
            outlineColor = "&H00FF0000",
            position = "top"
        )
        assertEquals(36, style.fontSize)
        assertEquals("&H000000FF", style.fontColor)
        assertEquals("&H00FF0000", style.outlineColor)
        assertEquals("top", style.position)
    }

    @Test
    fun `fontSize below 8 throws IllegalArgumentException`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            SubtitleBurnInStyle(fontSize = 7)
        }
        assertTrue(ex.message!!.contains("fontSize"))
    }

    @Test
    fun `fontSize above 72 throws IllegalArgumentException`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            SubtitleBurnInStyle(fontSize = 73)
        }
        assertTrue(ex.message!!.contains("fontSize"))
    }

    @Test
    fun `fontSize 8 is valid`() {
        val style = SubtitleBurnInStyle(fontSize = 8)
        assertEquals(8, style.fontSize)
    }

    @Test
    fun `fontSize 72 is valid`() {
        val style = SubtitleBurnInStyle(fontSize = 72)
        assertEquals(72, style.fontSize)
    }

    @Test
    fun `invalid fontColor format throws IllegalArgumentException`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            SubtitleBurnInStyle(fontColor = "white")
        }
        assertTrue(ex.message!!.contains("fontColor"))
    }

    @Test
    fun `invalid outlineColor format throws IllegalArgumentException`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            SubtitleBurnInStyle(outlineColor = "black")
        }
        assertTrue(ex.message!!.contains("outlineColor"))
    }

    @Test
    fun `invalid position throws IllegalArgumentException`() {
        val ex = assertThrows(IllegalArgumentException::class.java) {
            SubtitleBurnInStyle(position = "left")
        }
        assertTrue(ex.message!!.contains("position"))
    }

    @Test
    fun `toForceStyle produces correct FFmpeg force_style string for defaults`() {
        val style = SubtitleBurnInStyle()
        val forceStyle = style.toForceStyle()
        assertEquals(
            "FontSize=24,PrimaryColour=&H00FFFFFF,OutlineColour=&H00000000,Outline=1,Shadow=1,Alignment=2",
            forceStyle
        )
    }

    @Test
    fun `toForceStyle maps bottom to Alignment 2`() {
        val style = SubtitleBurnInStyle(position = "bottom")
        assertTrue(style.toForceStyle().contains("Alignment=2"))
    }

    @Test
    fun `toForceStyle maps top to Alignment 8`() {
        val style = SubtitleBurnInStyle(position = "top")
        assertTrue(style.toForceStyle().contains("Alignment=8"))
    }

    @Test
    fun `toForceStyle maps middle to Alignment 5`() {
        val style = SubtitleBurnInStyle(position = "middle")
        assertTrue(style.toForceStyle().contains("Alignment=5"))
    }

    @Test
    fun `toForceStyle includes custom fontSize`() {
        val style = SubtitleBurnInStyle(fontSize = 48)
        assertTrue(style.toForceStyle().contains("FontSize=48"))
    }

    @Test
    fun `toForceStyle includes custom fontColor`() {
        val style = SubtitleBurnInStyle(fontColor = "&H000000FF")
        assertTrue(style.toForceStyle().contains("PrimaryColour=&H000000FF"))
    }

    @Test
    fun `toForceStyle includes custom outlineColor`() {
        val style = SubtitleBurnInStyle(outlineColor = "&H00FF0000")
        assertTrue(style.toForceStyle().contains("OutlineColour=&H00FF0000"))
    }
}
