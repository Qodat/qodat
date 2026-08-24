package stan.qodat.scene.control.export.gif.encoder

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FastGifEncoderRoundTripTest {

    @Test
    fun writesGif89aHeaderScreenSizeLoopAndTrailer() {
        val width = 3
        val height = 2
        val frame = IntArray(width * height) { 0xFF336699.toInt() }
        val gif = encodeGif(width, height, loopCount = 4, frame, delayCentiSeconds = 8, dither = false)
        assertEquals("GIF89a", String(gif, 0, 6, StandardCharsets.US_ASCII))
        assertEquals(width, u16le(gif, 6))
        assertEquals(height, u16le(gif, 8))
        assertEquals(4, netscapeLoopCount(gif))
        assertEquals(0x3B, gif.last().toInt() and 0xFF)
    }

    @Test
    fun emptyTransparentFrameEncodes() {
        val width = 2
        val height = 2
        val frame = IntArray(width * height)
        val palette = FastGifEncoder.buildPalette(arrayOf(frame))
        assertFalse(palette.isReduced)
        val gif = encodeGif(width, height, 0, frame, 5, dither = false, palette)
        assertEquals("GIF89a", String(gif, 0, 6, StandardCharsets.US_ASCII))
        assertEquals(0, netscapeLoopCount(gif))
        assertEquals(0x3B, gif.last().toInt() and 0xFF)
    }

    @Test
    fun singleColorFrameRoundTripsThroughEncoder() {
        val width = 4
        val height = 1
        val red = 0xFFFF0000.toInt()
        val frame = IntArray(width * height) { red }
        val palette = FastGifEncoder.buildPalette(arrayOf(frame))
        assertFalse(palette.isReduced)
        assertEquals(0, palette.rgb[0])
        assertTrue(palette.rgb.any { it == 0xFF0000 })

        val out = ByteArrayOutputStream()
        val encoder = FastGifEncoder(out, width, height, 0, palette, false)
        val encoded = encoder.encodeFrame(frame, 1)
        assertEquals(2, encoded.delayCentiSeconds)
        assertTrue(encoded.lzwData.isNotEmpty())
        encoder.writeFrame(encoded)
        encoder.finish()
        val gif = out.toByteArray()
        assertEquals("GIF89a", String(gif, 0, 6, StandardCharsets.US_ASCII))
        assertEquals(0, netscapeLoopCount(gif))
    }

    @Test
    fun ditheredSingleColorFrameWritesValidGif() {
        val width = 2
        val height = 2
        val frame = IntArray(width * height) { 0xFF00AA55.toInt() }
        val gif = encodeGif(width, height, 1, frame, 10, dither = true)
        assertEquals("GIF89a", String(gif, 0, 6, StandardCharsets.US_ASCII))
        assertEquals(1, netscapeLoopCount(gif))
        assertEquals(0x3B, gif.last().toInt() and 0xFF)
    }

    @Test
    fun rejectsFrameSizeMismatch() {
        val frame = intArrayOf(0xFF112233.toInt())
        val palette = FastGifEncoder.buildPalette(arrayOf(frame))
        val encoder = FastGifEncoder(ByteArrayOutputStream(), 2, 2, 0, palette, false)
        assertFailsWith<IllegalArgumentException> { encoder.encodeFrame(frame, 5) }
        assertFailsWith<IllegalArgumentException> { encoder.encodeFrame(IntArray(0), 5) }
    }

    private fun encodeGif(
        width: Int,
        height: Int,
        loopCount: Int,
        frame: IntArray,
        delayCentiSeconds: Int,
        dither: Boolean,
        palette: FastPalette = FastGifEncoder.buildPalette(arrayOf(frame)),
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val encoder = FastGifEncoder(out, width, height, loopCount, palette, dither)
        encoder.writeFrame(encoder.encodeFrame(frame, delayCentiSeconds))
        encoder.finish()
        return out.toByteArray()
    }

    private fun u16le(bytes: ByteArray, offset: Int): Int {
        return (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun netscapeLoopCount(gif: ByteArray): Int {
        val marker = "NETSCAPE2.0".toByteArray(StandardCharsets.US_ASCII)
        outer@ for (i in 0..gif.size - marker.size - 4) {
            for (j in marker.indices) {
                if (gif[i + j] != marker[j]) continue@outer
            }
            return u16le(gif, i + marker.size + 2)
        }
        throw AssertionError("missing NETSCAPE2.0 loop block")
    }
}
