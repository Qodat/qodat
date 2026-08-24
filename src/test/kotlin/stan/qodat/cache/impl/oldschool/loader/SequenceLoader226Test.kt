package stan.qodat.cache.impl.oldschool.loader

import com.displee.io.impl.OutputBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SequenceLoader226Test {

    @Test
    fun decodesFrameTablesAndStep() {
        val bytes = OutputBuffer(16).apply {
            writeByte(1)
            writeShort(2)
            writeShort(5)
            writeShort(6)
            writeShort(1)
            writeShort(3)
            writeShort(2)
            writeShort(4)
            writeByte(2)
            writeShort(10)
            writeByte(12)
            writeByte(1)
            writeShort(7)
            writeShort(8)
            writeByte(0)
        }.array()

        val def = SequenceLoader226().load(42, bytes)
        assertEquals("42", def.id)
        assertTrue(def.frameLenghts.contentEquals(intArrayOf(5, 6)))
        assertTrue(def.frameIDs.contentEquals(intArrayOf(1 + (2 shl 16), 3 + (4 shl 16))))
        assertEquals(10, def.frameStep)
        assertTrue(def.chatFrameIds.contentEquals(intArrayOf(7 + (8 shl 16))))
    }

    @Test
    fun configureForRevisionSwitchesMayaAndSoundOpcodes() {
        val pre226 = OutputBuffer(16).apply {
            writeByte(13)
            writeByte(0)
            writeByte(14)
            writeInt(99)
            writeByte(0)
        }.array()
        val older = SequenceLoader226().load(1, pre226)
        assertEquals(99, older.animMayaId)

        val post226Bytes = OutputBuffer(16).apply {
            writeByte(13)
            writeInt(77)
            writeByte(0)
        }.array()
        val newer = SequenceLoader226().apply { configureForRevision(1269) }.load(2, post226Bytes)
        assertEquals(77, newer.animMayaId)
    }
}
