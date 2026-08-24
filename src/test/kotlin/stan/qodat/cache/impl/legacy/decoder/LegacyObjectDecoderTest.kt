package stan.qodat.cache.impl.legacy.decoder

import qodat.cache.io.InputStream
import qodat.cache.io.OutputStream
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LegacyObjectDecoderTest {

    @Test
    fun decodesModelsNameAnimationAndRecolors() {
        val bytes = OutputStream().apply {
            writeByte(1)
            writeByte(1)
            writeShort(70)
            writeByte(10)
            writeByte(2)
            writeStringOld("Tree")
            writeByte(24)
            writeShort(15)
            writeByte(40)
            writeByte(1)
            writeShort(0x1001)
            writeShort(0x2002)
            writeByte(0)
        }.flip()

        val obj = LegacyObjectDecoder().load(1276, InputStream(bytes))
        assertEquals("Tree", obj.name)
        assertTrue(obj.modelIds.contentEquals(arrayOf("70")))
        assertTrue(obj.animationIds.contentEquals(arrayOf("15")))
        assertTrue(obj.findColor!!.contentEquals(shortArrayOf(0x1001)))
        assertTrue(obj.replaceColor!!.contentEquals(shortArrayOf(0x2002.toShort())))
    }

    @Test
    fun unsetAnimationStaysMinusOneAndModelOnlyOpcodeWorks() {
        val bytes = OutputStream().apply {
            writeByte(5)
            writeByte(2)
            writeShort(8)
            writeShort(9)
            writeByte(24)
            writeShort(0xFFFF)
            writeByte(0)
        }.flip()

        val obj = LegacyObjectDecoder().load(2, InputStream(bytes))
        assertTrue(obj.modelIds.contentEquals(arrayOf("8", "9")))
        assertTrue(obj.animationIds.contentEquals(arrayOf("-1")))
    }

    @Test
    fun hiddenActionDoesNotChangeReturnedDefinition() {
        val bytes = OutputStream().apply {
            writeByte(2)
            writeStringOld("Chest")
            writeByte(30)
            writeStringOld("Hidden")
            writeByte(31)
            writeStringOld("Open")
            writeByte(0)
        }.flip()

        val obj = LegacyObjectDecoder().load(3, InputStream(bytes))
        assertEquals("Chest", obj.name)
        assertTrue(obj.modelIds.isEmpty())
    }

    private fun OutputStream.writeStringOld(value: String) {
        writeBytes(value.toByteArray(StandardCharsets.ISO_8859_1))
        writeByte(10)
    }
}
