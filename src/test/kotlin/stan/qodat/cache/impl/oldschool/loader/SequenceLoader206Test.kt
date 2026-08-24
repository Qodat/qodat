package stan.qodat.cache.impl.oldschool.loader

import qodat.cache.io.OutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SequenceLoader206Test {

    @Test
    fun decodesFrameTablesWithoutSwappingLengthsAndIds() {
        val bytes = OutputStream().apply {
            writeByte(1)
            writeFrameTables(
                lengths = intArrayOf(5, 40),
                fileIds = intArrayOf(1, 2),
                archiveIds = intArrayOf(3, 4),
            )
            writeByte(2)
            writeShort(12)
            writeByte(0)
        }.flip()

        val def = SequenceLoader206().load(7, bytes)
        assertEquals("7", def.id)
        assertTrue(def.frameLenghts.contentEquals(intArrayOf(5, 40)))
        assertTrue(def.frameIDs.contentEquals(intArrayOf(packed(1, 3), packed(2, 4))))
        assertEquals(12, def.frameStep)
    }

    @Test
    fun emptyFrameAndChatTablesStayEmpty() {
        val bytes = OutputStream().apply {
            writeByte(1)
            writeShort(0)
            writeByte(12)
            writeByte(0)
            writeByte(0)
        }.flip()

        val def = SequenceLoader206().load(0, bytes)
        assertEquals("0", def.id)
        assertTrue(def.frameLenghts.contentEquals(intArrayOf()))
        assertTrue(def.frameIDs.contentEquals(intArrayOf()))
        assertTrue(def.chatFrameIds.contentEquals(intArrayOf()))
    }

    @Test
    fun packsEdgeFrameIds() {
        val bytes = OutputStream().apply {
            writeByte(1)
            writeFrameTables(
                lengths = intArrayOf(0, 65535),
                fileIds = intArrayOf(0, 65535),
                archiveIds = intArrayOf(1, 65535),
            )
            writeByte(12)
            writeByte(1)
            writeShort(0)
            writeShort(65535)
            writeByte(0)
        }.flip()

        val def = SequenceLoader206().load(65535, bytes)
        assertEquals("65535", def.id)
        assertTrue(def.frameLenghts.contentEquals(intArrayOf(0, 65535)))
        assertTrue(def.frameIDs.contentEquals(intArrayOf(packed(0, 1), packed(65535, 65535))))
        assertTrue(def.chatFrameIds.contentEquals(intArrayOf(packed(0, 65535))))
    }

    @Test
    fun opcode13IsFrameSoundsNotMayaId() {
        val bytes = OutputStream().apply {
            writeByte(13)
            writeByte(2)
            write24(0x010203)
            write24(0x040506)
            writeByte(0)
        }.flip()

        val def = SequenceLoader206().load(1, bytes)
        assertTrue(def.frameSounds.contentEquals(intArrayOf(0x010203, 0x040506)))
        assertEquals(-1, def.animMayaId)
    }

    @Test
    fun opcode14SetsMayaIdAndSkips15Through17Payloads() {
        val bytes = OutputStream().apply {
            writeByte(15)
            writeShort(1)
            writeShort(9)
            write24(0xAABBCC)
            writeByte(16)
            writeShort(3)
            writeShort(8)
            writeByte(17)
            writeByte(2)
            writeByte(10)
            writeByte(20)
            writeByte(14)
            writeInt(99)
            writeByte(18)
            writeCString("idle")
            writeByte(0)
        }.flip()

        val def = SequenceLoader206().load(3, bytes)
        assertEquals(99, def.animMayaId)
        assertEquals("idle", def.name)
        assertEquals(0, def.animMayaStart)
        assertEquals(0, def.animMayaEnd)
    }

    @Test
    fun interleaveLeaveAppendsSentinelAndFlagsStayDefaultUnlessSet() {
        val bytes = OutputStream().apply {
            writeByte(3)
            writeByte(2)
            writeByte(1)
            writeByte(4)
            writeByte(4)
            writeByte(6)
            writeShort(65535)
            writeByte(7)
            writeShort(0)
            writeByte(0)
        }.flip()

        val def = SequenceLoader206().load(2, bytes)
        assertTrue(def.interleaveLeave.contentEquals(intArrayOf(1, 4, 9999999)))
        assertTrue(def.stretches)
        assertEquals(65535, def.leftHandItem)
        assertEquals(0, def.rightHandItem)
        assertEquals(-1, def.frameStep)
        assertNull(def.frameIDs)
    }

    private fun packed(fileId: Int, archiveId: Int): Int = fileId + (archiveId shl 16)
}

private fun OutputStream.writeFrameTables(
    lengths: IntArray,
    fileIds: IntArray,
    archiveIds: IntArray,
) {
    writeShort(lengths.size)
    lengths.forEach { writeShort(it) }
    fileIds.forEach { writeShort(it) }
    archiveIds.forEach { writeShort(it) }
}

private fun OutputStream.write24(value: Int) {
    writeByte(value ushr 16)
    writeByte(value ushr 8)
    writeByte(value)
}

private fun OutputStream.writeCString(value: String) {
    value.forEach { writeByte(it.code) }
    writeByte(0)
}
