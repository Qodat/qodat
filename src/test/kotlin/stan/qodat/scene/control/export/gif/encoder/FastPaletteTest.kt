package stan.qodat.scene.control.export.gif.encoder

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FastPaletteTest {

    @Test
    fun emptyFramesReserveOnlyTransparentBlack() {
        val emptyArray = FastPalette.fromFrames(arrayOf(), 16)
        val emptyPixels = FastPalette.fromFrames(arrayOf(intArrayOf()), 16)
        val allBlack = FastPalette.fromFrames(arrayOf(intArrayOf(0xFF000000.toInt())), 16)
        for (palette in arrayOf(emptyArray, emptyPixels, allBlack)) {
            assertFalse(palette.isReduced)
            assertEquals(0, palette.transparentIndex)
            assertEquals(FastPalette.TRANSPARENT_RGB, palette.rgb[0])
            assertEquals(2, palette.paddedSize)
            assertEquals(1, Integer.bitCount(palette.paddedSize))
        }
    }

    @Test
    fun singleColorFrameKeepsOpaqueEntry() {
        val red = 0xFFFF0000.toInt()
        val palette = FastPalette.fromFrames(arrayOf(intArrayOf(red, red)), 16)
        assertFalse(palette.isReduced)
        assertEquals(0, palette.rgb[0])
        assertEquals(0xFF0000, palette.rgb[1])
        assertEquals(2, palette.paddedSize)
    }

    @Test
    fun invalidMaxColorsRejected() {
        val frames = arrayOf(intArrayOf(0xFF112233.toInt()))
        assertFailsWith<IllegalArgumentException> { FastPalette.fromFrames(frames, 1) }
        assertFailsWith<IllegalArgumentException> { FastPalette.fromFrames(frames, 257) }
    }

    @Test
    fun medianCutReducesWhenOverBudget() {
        val frame = IntArray(8) { i -> 0xFF000000.toInt() or ((i + 1) shl 16) }
        val palette = FastPalette.fromFrames(arrayOf(frame), 2)
        assertTrue(palette.isReduced)
        assertEquals(2, palette.paddedSize)
        assertEquals(0, palette.rgb[0])
    }
}
