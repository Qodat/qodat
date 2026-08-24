package stan.qodat.cache.impl.displee

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Frame-hash and widget-id packing used by [DispleeCache],
 * [stan.qodat.cache.impl.displee.types.AnimManager], and [stan.qodat.cache.impl.qodat.QodatCache].
 */
class CacheIdPackingTest {

    @Test
    fun splitsFrameHashHexIntoArchiveAndFileIds() {
        val hash = (0x001A shl 16) or 0x000B
        val hex = Integer.toHexString(hash)
        assertEquals("1a000b", hex)
        assertEquals(0x1A, getFileId(hex))
        assertEquals(0x0B, getFrameId(hex))
    }

    @Test
    fun splitsShortHexWhenHighWordIsSmall() {
        val hash = (1 shl 16) or 2
        val hex = Integer.toHexString(hash)
        assertEquals("10002", hex)
        assertEquals(1, getFileId(hex))
        assertEquals(2, getFrameId(hex))
    }

    @Test
    fun qodatEncodePackingRoundTripsThroughHexSplit() {
        val archiveId = 0x00F3
        val frameIndex = 0x0012
        val packed = packQodatFrameHash(archiveId, frameIndex)
        val hex = Integer.toHexString(packed)
        assertEquals(archiveId, getFileId(hex))
        assertEquals(frameIndex, getFrameId(hex))
    }

    @Test
    fun masksArchiveAndIndexTo16BitsWhenPacking() {
        assertEquals(0x0002_0003, packQodatFrameHash(0x1_0002, 0x1_0003))
        val hex = Integer.toHexString(0x0002_0003)
        assertEquals(2, getFileId(hex))
        assertEquals(3, getFrameId(hex))
    }

    @Test
    fun packsInterfaceWidgetIdFromArchiveAndFile() {
        assertEquals(0x0005_0007, packWidgetId(5, 7))
        assertEquals(5, packWidgetId(5, 7) shr 16)
        assertEquals(7, packWidgetId(5, 7) and 0xFFFF)
        assertEquals(0, packWidgetId(0, 0))
    }

    @Test
    fun hexShorterThanFiveDigitsCannotYieldAFileId() {
        assertFailsWith<StringIndexOutOfBoundsException> {
            getFileId(Integer.toHexString(0xABC))
        }
        assertFailsWith<NumberFormatException> {
            getFileId(Integer.toHexString(0xABCD))
        }
    }

    companion object {
        internal fun getFileId(hexString: String): Int =
            Integer.parseInt(hexString.substring(0, hexString.length - 4), 16)

        internal fun getFrameId(hexString: String): Int =
            Integer.parseInt(hexString.substring(hexString.length - 4), 16)

        internal fun packQodatFrameHash(frameArchiveId: Int, index: Int): Int =
            ((frameArchiveId and 0xFFFF) shl 16) or (index and 0xFFFF)

        internal fun packWidgetId(archiveId: Int, fileId: Int): Int =
            (archiveId shl 16) + fileId
    }
}
