package stan.qodat.cache.impl.oldschool.loader

import qodat.cache.io.OutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnimationFrameCodecTest {

    @Test
    fun detects317MagicOnFramesAndFramemaps() {
        val frame = byteArrayOf(AnimationFrameCodec.NR_317_MAGIC, AnimationFrameCodec.NR_317_MAGIC, 0)
        val framemap = byteArrayOf(0, 1, AnimationFrameCodec.NR_317_MAGIC, AnimationFrameCodec.NR_317_MAGIC)
        assertTrue(AnimationFrameCodec.is317Frame(frame))
        assertTrue(AnimationFrameCodec.is317Framemap(framemap))
        assertFalse(AnimationFrameCodec.is317Frame(byteArrayOf(0, 1)))
        assertFalse(AnimationFrameCodec.is317Framemap(byteArrayOf(0, 1)))
        assertFalse(AnimationFrameCodec.is317Frame(byteArrayOf(AnimationFrameCodec.NR_317_MAGIC)))
    }

    @Test
    fun framemapIdUsesArchiveIdFor317AndPayloadForOsrs() {
        val nr317 = byteArrayOf(AnimationFrameCodec.NR_317_MAGIC, AnimationFrameCodec.NR_317_MAGIC, 0)
        val osrs = byteArrayOf(0x00, 0x2A, 0x00)
        assertEquals(17, AnimationFrameCodec.framemapId(nr317, 17))
        assertEquals(42, AnimationFrameCodec.framemapId(osrs, 99))
    }

    @Test
    fun read317ShortAppliesNrWrap() {
        assertEquals(1, AnimationFrameCodec.read317Short(byteArrayOf(0x00, 0x01), 0))
        assertEquals(32767, AnimationFrameCodec.read317Short(byteArrayOf(0x7F, 0xFF.toByte()), 0))
        assertEquals(-32769, AnimationFrameCodec.read317Short(byteArrayOf(0x80.toByte(), 0x00), 0))
        assertEquals(-2, AnimationFrameCodec.read317Short(byteArrayOf(0xFF.toByte(), 0xFF.toByte()), 0))
    }

    @Test
    fun decodesOsrsFramemapAndFrame() {
        val framemapBytes = OutputStream().apply {
            writeByte(2)
            writeByte(0)
            writeByte(3)
            writeByte(2)
            writeByte(1)
            writeByte(1)
            writeByte(2)
            writeByte(3)
        }.flip()
        val framemap = AnimationFrameCodec.loadFramemap(9, framemapBytes)
        assertEquals(2, framemap.length)
        assertEquals(0, framemap.types[0])
        assertEquals(3, framemap.types[1])
        assertTrue(framemap.frameMaps[0].contentEquals(intArrayOf(1, 2)))
        assertTrue(framemap.frameMaps[1].contentEquals(intArrayOf(3)))

        val frameBytes = OutputStream().apply {
            writeShort(9)
            writeByte(2)
            writeByte(1)
            writeByte(7)
            writeByte(10 + 64)
            writeByte(20 + 64)
            writeByte(30 + 64)
            writeByte(40 + 64)
        }.flip()
        val frame = AnimationFrameCodec.loadFrame(framemap, 4, frameBytes)
        assertEquals(4, frame.id)
        assertEquals(2, frame.translatorCount)
        assertEquals(0, frame.indexFrameIds[0])
        assertEquals(1, frame.indexFrameIds[1])
        assertEquals(10, frame.translator_x[0])
        assertEquals(0, frame.translator_y[0])
        assertEquals(0, frame.translator_z[0])
        assertEquals(20, frame.translator_x[1])
        assertEquals(30, frame.translator_y[1])
        assertEquals(40, frame.translator_z[1])
        assertFalse(frame.showing)
    }

    @Test
    fun decodes317FramemapAndFrameAndMarksType5Alpha() {
        val framemapBytes = OutputStream().apply {
            writeShort(2)
            writeShort(0)
            writeShort(5)
            writeShort(1)
            writeShort(1)
            writeShort(4)
            writeShort(7)
            writeByte(AnimationFrameCodec.NR_317_MAGIC.toInt())
            writeByte(AnimationFrameCodec.NR_317_MAGIC.toInt())
        }.flip()
        val framemap = AnimationFrameCodec.loadFramemap(3, framemapBytes)
        assertEquals(2, framemap.length)
        assertEquals(5, framemap.types[1])
        assertTrue(framemap.frameMaps[0].contentEquals(intArrayOf(4)))

        val frameBytes = byteArrayOf(
            AnimationFrameCodec.NR_317_MAGIC,
            AnimationFrameCodec.NR_317_MAGIC,
            2,
            0,
            1,
            0x00, 0x0C,
        )
        val frame = AnimationFrameCodec.loadFrame(framemap, 8, frameBytes)
        assertEquals(2, frame.translatorCount)
        assertEquals(0, frame.indexFrameIds[0])
        assertEquals(0, frame.translator_x[0])
        assertEquals(1, frame.indexFrameIds[1])
        assertEquals(12, frame.translator_x[1])
        assertEquals(0, frame.translator_y[1])
        assertEquals(0, frame.translator_z[1])
        assertTrue(frame.showing)
    }
}
