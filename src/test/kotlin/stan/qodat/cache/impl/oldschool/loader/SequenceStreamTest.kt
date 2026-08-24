package stan.qodat.cache.impl.oldschool.loader

import com.displee.io.impl.InputBuffer
import com.displee.io.impl.OutputBuffer
import kotlin.test.Test
import kotlin.test.assertTrue

class SequenceStreamTest {

    @Test
    fun readFrameLengthAndIdTablesKeepsLengthsSeparateFromPackedIds() {
        val bytes = OutputBuffer(16).apply {
            writeShort(2)
            writeShort(10)
            writeShort(20)
            writeShort(1)
            writeShort(2)
            writeShort(3)
            writeShort(4)
        }.array()

        val (lengths, ids) = InputBuffer(bytes).readFrameLengthAndIdTables()
        assertTrue(lengths.contentEquals(intArrayOf(10, 20)))
        assertTrue(ids.contentEquals(intArrayOf(packed(1, 3), packed(2, 4))))
    }

    @Test
    fun readFrameLengthAndIdTablesEmptyCountYieldsEmptyArrays() {
        val bytes = OutputBuffer(16).apply { writeShort(0) }.array()

        val (lengths, ids) = InputBuffer(bytes).readFrameLengthAndIdTables()
        assertTrue(lengths.isEmpty())
        assertTrue(ids.isEmpty())
    }

    @Test
    fun readFrameLengthAndIdTablesPacksZeroAndMaxArchiveFileIds() {
        val bytes = OutputBuffer(16).apply {
            writeShort(2)
            writeShort(0)
            writeShort(65535)
            writeShort(0)
            writeShort(65535)
            writeShort(1)
            writeShort(65535)
        }.array()

        val (lengths, ids) = InputBuffer(bytes).readFrameLengthAndIdTables()
        assertTrue(lengths.contentEquals(intArrayOf(0, 65535)))
        assertTrue(ids.contentEquals(intArrayOf(packed(0, 1), packed(65535, 65535))))
    }

    @Test
    fun readPackedArchiveFileIdsUsesByteCountAndSamePacking() {
        val bytes = OutputBuffer(16).apply {
            writeByte(2)
            writeShort(7)
            writeShort(8)
            writeShort(9)
            writeShort(10)
        }.array()

        val ids = InputBuffer(bytes).readPackedArchiveFileIds()
        assertTrue(ids.contentEquals(intArrayOf(packed(7, 9), packed(8, 10))))
    }

    @Test
    fun readPackedArchiveFileIdsEmptyCountYieldsEmptyArray() {
        val bytes = OutputBuffer(16).apply { writeByte(0) }.array()
        assertTrue(InputBuffer(bytes).readPackedArchiveFileIds().isEmpty())
    }

    private fun packed(fileId: Int, archiveId: Int): Int = fileId + (archiveId shl 16)
}
