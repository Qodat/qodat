package stan.qodat.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class HslPaletteRoundTripTest {

    @Test
    fun encodeDecodeRoundTripsPaletteEntriesWithoutMutatingSingleton() {
        val indices = intArrayOf(0, 1, 127, 128, 255, 512, 1024, 4096, 16384, 32767, 40000, 65535)
        val snapshot = IntArray(indices.size) { HslPalette.rgb(indices[it]) }
        for (i in indices.indices) {
            val rgb = snapshot[i]
            val encoded = HslPalette.encode(rgb shr 16 and 0xFF, rgb shr 8 and 0xFF, rgb and 0xFF)
            assertEquals(rgb, HslPalette.rgb(encoded), "hsl=${indices[i]} encoded=$encoded")
        }
        for (i in indices.indices) {
            assertEquals(snapshot[i], HslPalette.rgb(indices[i]))
        }
    }

    @Test
    fun encodeOfArbitraryRgbIsIdempotent() {
        val colors = intArrayOf(0x000000, 0xFF0000, 0x00FF00, 0x0000FF, 0x808080, 0x123456, 0xFFFFFF)
        for (packed in colors) {
            val encoded = HslPalette.encode(packed shr 16 and 0xFF, packed shr 8 and 0xFF, packed and 0xFF)
            assertTrue(encoded in 0..65535)
            val rgb = HslPalette.rgb(encoded)
            val encodedAgain = HslPalette.encode(rgb shr 16 and 0xFF, rgb shr 8 and 0xFF, rgb and 0xFF)
            assertEquals(rgb, HslPalette.rgb(encodedAgain))
        }
    }

    @Test
    fun rgbAndEncodeAcceptOutOfRangeInputs() {
        assertEquals(HslPalette.rgb(7), HslPalette.rgb(7 + 65536))
        assertEquals(HslPalette.rgb(7), HslPalette.rgb(7 - 65536))
        assertTrue(HslPalette.encode(-10, 300, 999) in 0..65535)
        assertNotEquals(0, HslPalette.rgb(0))
    }
}
