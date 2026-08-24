package stan.qodat.cache.impl.oldschool.loader

import net.runelite.cache.io.InputStream
import net.runelite.cache.io.OutputStream
import kotlin.test.Test
import kotlin.test.assertTrue

class SequenceStreamTest {

    @Test
    fun readFrameLengthAndIdTablesKeepsLengthsSeparateFromPackedIds() {
        val bytes = OutputStream().apply {
            writeShort(2)
            writeShort(10)
            writeShort(20)
            writeShort(1)
            writeShort(2)
            writeShort(3)
            writeShort(4)
        }.flip()

        val (lengths, ids) = InputStream(bytes).readFrameLengthAndIdTables()
        assertTrue(lengths.contentEquals(intArrayOf(10, 20)))
        assertTrue(ids.contentEquals(intArrayOf(packed(1, 3), packed(2, 4))))
    }

    @Test
    fun readFrameLengthAndIdTablesEmptyCountYieldsEmptyArrays() {
        val bytes = OutputStream().apply { writeShort(0) }.flip()

        val (lengths, ids) = InputStream(bytes).readFrameLengthAndIdTables()
        assertTrue(lengths.isEmpty())
        assertTrue(ids.isEmpty())
    }

    @Test
    fun readFrameLengthAndIdTablesPacksZeroAndMaxArchiveFileIds() {
        val bytes = OutputStream().apply {
            writeShort(2)
            writeShort(0)
            writeShort(65535)
            writeShort(0)
            writeShort(65535)
            writeShort(1)
            writeShort(65535)
        }.flip()

        val (lengths, ids) = InputStream(bytes).readFrameLengthAndIdTables()
        assertTrue(lengths.contentEquals(intArrayOf(0, 65535)))
        assertTrue(ids.contentEquals(intArrayOf(packed(0, 1), packed(65535, 65535))))
    }

    @Test
    fun readPackedArchiveFileIdsUsesByteCountAndSamePacking() {
        val bytes = OutputStream().apply {
            writeByte(2)
            writeShort(7)
            writeShort(8)
            writeShort(9)
            writeShort(10)
        }.flip()

        val ids = InputStream(bytes).readPackedArchiveFileIds()
        assertTrue(ids.contentEquals(intArrayOf(packed(7, 9), packed(8, 10))))
    }

    @Test
    fun readPackedArchiveFileIdsEmptyCountYieldsEmptyArray() {
        val bytes = OutputStream().apply { writeByte(0) }.flip()
        assertTrue(InputStream(bytes).readPackedArchiveFileIds().isEmpty())
    }

    private fun packed(fileId: Int, archiveId: Int): Int = fileId + (archiveId shl 16)
}
