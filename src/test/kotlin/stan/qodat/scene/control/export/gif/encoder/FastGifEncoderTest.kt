package stan.qodat.scene.control.export.gif.encoder

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FastGifEncoderTest {

    @Test
    fun buildPaletteReservesBlackAndFitsPowerOfTwo() {
        val frame = intArrayOf(0xFF000000.toInt(), 0xFFFF0000.toInt(), 0xFF00FF00.toInt())
        val palette = FastGifEncoder.buildPalette(arrayOf(frame))
        assertFalse(palette.isReduced)
        assertEquals(0, palette.rgb[0])
        assertTrue(palette.paddedSize >= 4)
        assertEquals(1, Integer.bitCount(palette.paddedSize))
    }

    @Test
    fun encodesGif89aWithNetscapeLoopAndTrailer() {
        val width = 4
        val height = 2
        val frame = IntArray(width * height) { index ->
            if (index == 0) 0xFF000000.toInt() else (0xFF202060.toInt() or (index shl 8))
        }
        val palette = FastGifEncoder.buildPalette(arrayOf(frame))
        val out = ByteArrayOutputStream()
        val encoder = FastGifEncoder(out, width, height, 0, palette, palette.isReduced)
        encoder.writeFrame(encoder.encodeFrame(frame, 5))
        encoder.finish()
        val gif = out.toByteArray()
        assertTrue(gif.size > 16)
        assertEquals("GIF89a", String(gif, 0, 6, StandardCharsets.US_ASCII))
        assertEquals(0x3B, gif.last().toInt() and 0xFF)
        assertTrue(String(gif, StandardCharsets.ISO_8859_1).contains("NETSCAPE2.0"))
    }

    @Test
    fun medianCutMarksReducedPalette() {
        val frame = IntArray(300) { i ->
            0xFF000000.toInt() or ((i + 1) shl 8)
        }
        val palette = FastGifEncoder.buildPalette(arrayOf(frame))
        assertTrue(palette.isReduced)
        assertTrue(palette.paddedSize <= 256)
    }
}
