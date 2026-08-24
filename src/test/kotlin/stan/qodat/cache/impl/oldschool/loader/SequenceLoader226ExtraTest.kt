package stan.qodat.cache.impl.oldschool.loader

import com.displee.io.impl.OutputBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SequenceLoader226ExtraTest {

    @Test
    fun emptyFrameAndChatTablesStayEmpty() {
        val bytes = OutputBuffer(16).apply {
            writeByte(1)
            writeShort(0)
            writeByte(12)
            writeByte(0)
            writeByte(0)
        }.array()

        val def = SequenceLoader226().load(0, bytes)
        assertEquals("0", def.id)
        assertTrue(def.frameLenghts.contentEquals(intArrayOf()))
        assertTrue(def.frameIDs.contentEquals(intArrayOf()))
        assertTrue(def.chatFrameIds.contentEquals(intArrayOf()))
    }

    @Test
    fun packsEdgeFrameIdsWithoutSwappingTables() {
        val bytes = OutputBuffer(16).apply {
            writeByte(1)
            writeFrameTables(
                lengths = intArrayOf(1, 65535),
                fileIds = intArrayOf(0, 65535),
                archiveIds = intArrayOf(2, 1),
            )
            writeByte(0)
        }.array()

        val def = SequenceLoader226().load(65535, bytes)
        assertTrue(def.frameLenghts.contentEquals(intArrayOf(1, 65535)))
        assertTrue(def.frameIDs.contentEquals(intArrayOf(packed(0, 2), packed(65535, 1))))
    }

    @Test
    fun pre226Opcode15MapsSoundsAnd16SetsMayaRange() {
        val bytes = OutputBuffer(16).apply {
            writeByte(15)
            writeShort(1)
            writeShort(7)
            write24(packed24Sound(id = 5, loops = 3, location = 2))
            writeByte(16)
            writeShort(10)
            writeShort(20)
            writeByte(0)
        }.array()

        val def = SequenceLoader226().load(1, bytes)
        val sound = def.sounds?.get(7)
        assertEquals(5, sound?.id)
        assertEquals(3, sound?.loops)
        assertEquals(2, sound?.location)
        assertEquals(0, sound?.retain)
        assertEquals(-1, sound?.weight)
        assertEquals(10, def.animMayaStart)
        assertEquals(20, def.animMayaEnd)
        assertEquals(-1, def.animMayaId)
        assertEquals(0, def.verticalOffset)
    }

    @Test
    fun post226Opcode15SetsMayaRangeAnd16SetsVerticalOffset() {
        val bytes = OutputBuffer(16).apply {
            writeByte(15)
            writeShort(4)
            writeShort(9)
            writeByte(16)
            writeByte(-3)
            writeByte(0)
        }.array()

        val def = SequenceLoader226().apply { configureForRevision(1269) }.load(2, bytes)
        assertEquals(4, def.animMayaStart)
        assertEquals(9, def.animMayaEnd)
        assertEquals(-3, def.verticalOffset)
        assertNull(def.sounds)
    }

    @Test
    fun revision1268KeepsPre226MayaOnOpcode14() {
        val bytes = OutputBuffer(16).apply {
            writeByte(14)
            writeInt(55)
            writeByte(0)
        }.array()

        val atBoundary = SequenceLoader226().apply { configureForRevision(1268) }.load(3, bytes)
        assertEquals(55, atBoundary.animMayaId)
        assertNull(atBoundary.sounds)
    }

    @Test
    fun post226Opcode14MapsSoundsWithWeight() {
        val bytes = OutputBuffer(16).apply {
            writeByte(14)
            writeShort(1)
            writeShort(0)
            writeShort(12)
            writeByte(8)
            writeByte(2)
            writeByte(3)
            writeByte(4)
            writeByte(0)
        }.array()

        val def = SequenceLoader226().apply { configureForRevision(1269) }.load(4, bytes)
        val sound = def.sounds?.get(0)
        assertEquals(12, sound?.id)
        assertEquals(8, sound?.weight)
        assertEquals(2, sound?.loops)
        assertEquals(3, sound?.location)
        assertEquals(4, sound?.retain)
        assertEquals(-1, def.animMayaId)
    }

    @Test
    fun rev220BoundarySwitchesSoundLayout() {
        val oldBits = packed24Sound(id = 6, loops = 2, location = 1)
        val pre220 = OutputBuffer(16).apply {
            writeByte(13)
            writeByte(1)
            write24(oldBits)
            writeByte(0)
        }.array()
        val oldSound = SequenceLoader226().apply { configureForRevision(1141) }.load(5, pre220).sounds?.get(0)
        assertEquals(6, oldSound?.id)
        assertEquals(2, oldSound?.loops)
        assertEquals(1, oldSound?.location)
        assertEquals(0, oldSound?.retain)
        assertEquals(-1, oldSound?.weight)

        val post220 = OutputBuffer(16).apply {
            writeByte(13)
            writeByte(1)
            writeShort(9)
            writeByte(4)
            writeByte(5)
            writeByte(6)
            writeByte(0)
        }.array()
        val newSound = SequenceLoader226().apply { configureForRevision(1142) }.load(6, post220).sounds?.get(0)
        assertEquals(9, newSound?.id)
        assertEquals(4, newSound?.loops)
        assertEquals(5, newSound?.location)
        assertEquals(6, newSound?.retain)
        assertEquals(-1, newSound?.weight)
    }

    @Test
    fun mayaMasksNameAndCrossWorldFlag() {
        val bytes = OutputBuffer(16).apply {
            writeByte(17)
            writeByte(2)
            writeByte(0)
            writeByte(255)
            writeByte(18)
            writeCString("walk")
            writeByte(19)
            writeByte(0)
        }.array()

        val def = SequenceLoader226().load(8, bytes)
        assertEquals(256, def.animMayaMasks!!.size)
        assertTrue(def.animMayaMasks!![0])
        assertTrue(def.animMayaMasks!![255])
        assertFalse(def.animMayaMasks!![1])
        assertEquals("walk", def.name)
        assertTrue(def.soundsCrossWorldView)
    }

    @Test
    fun invalidRev220SoundIsNullEntry() {
        val bytes = OutputBuffer(16).apply {
            writeByte(13)
            writeByte(1)
            writeShort(0)
            writeByte(1)
            writeByte(0)
            writeByte(0)
            writeByte(0)
        }.array()

        val def = SequenceLoader226().apply { configureForRevision(1142) }.load(9, bytes)
        assertEquals(1, def.sounds!!.size)
        assertNull(def.sounds!![0])
    }

    private fun packed(fileId: Int, archiveId: Int): Int = fileId + (archiveId shl 16)

    private fun packed24Sound(id: Int, loops: Int, location: Int): Int =
        (id shl 8) or ((loops and 7) shl 4) or (location and 15)
}

private fun OutputBuffer.writeFrameTables(
    lengths: IntArray,
    fileIds: IntArray,
    archiveIds: IntArray,
) {
    writeShort(lengths.size)
    lengths.forEach { writeShort(it) }
    fileIds.forEach { writeShort(it) }
    archiveIds.forEach { writeShort(it) }
}

private fun OutputBuffer.write24(value: Int) {
    writeByte(value ushr 16)
    writeByte(value ushr 8)
    writeByte(value)
}

private fun OutputBuffer.writeCString(value: String) {
    value.forEach { writeByte(it.code) }
    writeByte(0)
}
