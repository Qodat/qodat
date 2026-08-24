package stan.qodat.cache.impl.legacy.decoder

import com.displee.io.impl.InputBuffer
import com.displee.io.impl.OutputBuffer
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LegacyNpcDecoderTest {

    @Test
    fun decodesModelsNameAnimsAndRecolors() {
        val bytes = OutputBuffer(16).apply {
            writeByte(1)
            writeByte(2)
            writeShort(30)
            writeShort(31)
            writeByte(2)
            writeStringOld("Goblin")
            writeByte(13)
            writeShort(8)
            writeByte(17)
            writeShort(9)
            writeShort(10)
            writeShort(11)
            writeShort(12)
            writeByte(40)
            writeByte(1)
            writeShort(0x00AA)
            writeShort(0x00BB)
            writeByte(0)
        }.array()

        val npc = LegacyNpcDecoder().load(13, InputBuffer(bytes))
        assertEquals("Goblin", npc.name)
        assertTrue(npc.modelIds.contentEquals(arrayOf("30", "31")))
        assertTrue(npc.animationIds.contentEquals(arrayOf("9", "8", "12", "11", "10")))
        assertTrue(npc.findColor!!.contentEquals(shortArrayOf(0x00AA)))
        assertTrue(npc.replaceColor!!.contentEquals(shortArrayOf(0x00BB)))
    }

    @Test
    fun emptyPayloadUsesNullNameAndEmptyModels() {
        val bytes = OutputBuffer(16).apply { writeByte(0) }.array()
        val npc = LegacyNpcDecoder().load(1, InputBuffer(bytes))
        assertEquals("null", npc.name)
        assertTrue(npc.modelIds.isEmpty())
        assertTrue(npc.animationIds.contentEquals(arrayOf("-1", "-1", "-1", "-1", "-1")))
    }

    private fun OutputBuffer.writeStringOld(value: String) {
        writeBytes(value.toByteArray(StandardCharsets.ISO_8859_1))
        writeByte(10)
    }
}
