package stan.qodat.scene.control.export.gif.encoder

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GifMathTest {

    @Test
    fun isPowerOfTwoMatchesBitCount() {
        assertTrue(GifMath.isPowerOfTwo(1))
        assertTrue(GifMath.isPowerOfTwo(2))
        assertTrue(GifMath.isPowerOfTwo(256))
        assertFalse(GifMath.isPowerOfTwo(0))
        assertFalse(GifMath.isPowerOfTwo(3))
        assertFalse(GifMath.isPowerOfTwo(12))
    }

    @Test
    fun roundUpToPowerOfTwoPadsPaletteSizes() {
        assertEquals(1, GifMath.roundUpToPowerOfTwo(1))
        assertEquals(2, GifMath.roundUpToPowerOfTwo(2))
        assertEquals(4, GifMath.roundUpToPowerOfTwo(3))
        assertEquals(16, GifMath.roundUpToPowerOfTwo(9))
        assertEquals(256, GifMath.roundUpToPowerOfTwo(255))
        assertEquals(256, GifMath.roundUpToPowerOfTwo(256))
    }
}
