package stan.qodat.util

import net.runelite.cache.io.OutputStream
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IoExtensionsTest {

    @Test
    fun writeByteOrShortUsesASingleByteInTheSmallRange() {
        assertTrue(encoded(-64).contentEquals(byteArrayOf(0)))
        assertTrue(encoded(-1).contentEquals(byteArrayOf(63)))
        assertTrue(encoded(0).contentEquals(byteArrayOf(64)))
        assertTrue(encoded(63).contentEquals(byteArrayOf(127)))
    }

    @Test
    fun writeByteOrShortUsesABiasedShortOutsideTheByteRange() {
        assertTrue(encoded(-65).contentEquals(shortBytes(-65)))
        assertTrue(encoded(64).contentEquals(shortBytes(64)))
        assertTrue(encoded(-0x8000).contentEquals(shortBytes(-0x8000)))
        assertTrue(encoded(0x7FFF).contentEquals(shortBytes(0x7FFF)))
    }

    @Test
    fun writeByteOrShortRejectsValuesThatDoNotFitAShort() {
        assertFailsWith<IllegalArgumentException> { encoded(0x8000) }
        assertFailsWith<IllegalArgumentException> { encoded(-0x8001) }
    }

    private fun encoded(value: Int): ByteArray =
        OutputStream().apply { writeByteOrShort(value) }.flip()

    private fun shortBytes(value: Int): ByteArray {
        val biased = value + 49152
        return byteArrayOf((biased shr 8).toByte(), biased.toByte())
    }
}
