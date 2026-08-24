package qodat.cache.models

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RSModelLoaderTest {

    @Test
    fun detectsTypeTrailers() {
        assertTrue(RSModelLoader.isType3(byteArrayOf(0, (-1).toByte(), (-3).toByte())))
        assertTrue(RSModelLoader.isType2(byteArrayOf(0, (-1).toByte(), (-2).toByte())))
        assertTrue(RSModelLoader.isType1(byteArrayOf(0, (-1).toByte(), (-1).toByte())))
        assertFalse(RSModelLoader.isType3(byteArrayOf(0, 0)))
        assertFalse(RSModelLoader.isRS3(byteArrayOf(1, 1, 0)))
    }

    @Test
    fun isFlaggedUsesBitMask() {
        assertTrue(RSModelLoader.isFlagged(0b0101, 0b0100))
        assertFalse(RSModelLoader.isFlagged(0b0001, 0b0100))
    }
}
