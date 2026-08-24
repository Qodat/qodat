package stan.qodat.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HslPaletteTest {

    @Test
    fun reservesZeroAsNonTransparentRgb() {
        assertNotEquals(0, HslPalette.rgb(0))
    }

    @Test
    fun encodeRoundTripsKnownPaletteEntries() {
        val samples = intArrayOf(1, 128, 512, 4096, 32767, 40000)
        for (hsl in samples) {
            val rgb = HslPalette.rgb(hsl)
            val encoded = HslPalette.encode(rgb shr 16 and 0xFF, rgb shr 8 and 0xFF, rgb and 0xFF)
            assertEquals(rgb, HslPalette.rgb(encoded), "hsl=$hsl encoded=$encoded")
        }
    }

    @Test
    fun brightnessRebuildsPalette() {
        val mid = HslPalette.rgb(20000)
        HslPalette.setBrightness(0.6)
        try {
            assertNotEquals(mid, HslPalette.rgb(20000))
        } finally {
            HslPalette.setBrightness(HslPalette.DEFAULT_BRIGHTNESS)
        }
        assertEquals(mid, HslPalette.rgb(20000))
    }

    @Test
    fun rgbMasksTo16Bits() {
        assertEquals(HslPalette.rgb(42), HslPalette.rgb(42 + 65536))
        assertTrue(HslPalette.rgb(1) in 1..0xFFFFFF)
    }
}
