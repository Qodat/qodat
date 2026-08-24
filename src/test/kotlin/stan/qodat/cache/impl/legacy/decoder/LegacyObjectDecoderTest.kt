package stan.qodat.cache.impl.legacy.decoder

import com.displee.io.impl.InputBuffer
import com.displee.io.impl.OutputBuffer
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LegacyObjectDecoderTest {

    @Test
    fun decodesModelsNameAnimationAndRecolors() {
        val bytes = OutputBuffer(16).apply {
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
        }.array()

        val obj = LegacyObjectDecoder().load(1276, InputBuffer(bytes))
        assertEquals("Tree", obj.name)
        assertTrue(obj.modelIds.contentEquals(arrayOf("70")))
        assertTrue(obj.animationIds.contentEquals(arrayOf("15")))
        assertTrue(obj.findColor!!.contentEquals(shortArrayOf(0x1001)))
        assertTrue(obj.replaceColor!!.contentEquals(shortArrayOf(0x2002.toShort())))
    }

    @Test
    fun unsetAnimationStaysMinusOneAndModelOnlyOpcodeWorks() {
        val bytes = OutputBuffer(16).apply {
            writeByte(5)
            writeByte(2)
            writeShort(8)
            writeShort(9)
            writeByte(24)
            writeShort(0xFFFF)
            writeByte(0)
        }.array()

        val obj = LegacyObjectDecoder().load(2, InputBuffer(bytes))
        assertTrue(obj.modelIds.contentEquals(arrayOf("8", "9")))
        assertTrue(obj.animationIds.contentEquals(arrayOf("-1")))
    }

    @Test
    fun hiddenActionDoesNotChangeReturnedDefinition() {
        val bytes = OutputBuffer(16).apply {
            writeByte(2)
            writeStringOld("Chest")
            writeByte(30)
            writeStringOld("Hidden")
            writeByte(31)
            writeStringOld("Open")
            writeByte(0)
        }.array()

        val obj = LegacyObjectDecoder().load(3, InputBuffer(bytes))
        assertEquals("Chest", obj.name)
        assertTrue(obj.modelIds.isEmpty())
    }

    private fun OutputBuffer.writeStringOld(value: String) {
        writeBytes(value.toByteArray(StandardCharsets.ISO_8859_1))
        writeByte(10)
    }
}
