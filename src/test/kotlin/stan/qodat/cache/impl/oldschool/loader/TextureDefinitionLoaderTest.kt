package stan.qodat.cache.impl.oldschool.loader

import net.runelite.cache.definitions.loaders.TextureLoader
import com.displee.io.impl.OutputBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextureDefinitionLoaderTest {

    @Test
    fun decodesRev233SingleFileTexture() {
        val bytes = OutputBuffer(16).apply {
            writeShort(55)
            writeShort(0x1234)
            writeByte(1)
            writeByte(2)
            writeByte(3)
        }.array()

        val texture = TextureLoader().load(9, bytes)
        assertEquals(9, texture.id)
        assertTrue(texture.fileIds.contentEquals(intArrayOf(55)))
        assertEquals(0x1234, texture.missingColor)
        assertTrue(texture.field1778)
        assertEquals(2, texture.animationDirection)
        assertEquals(3, texture.animationSpeed)
    }

    @Test
    fun decodesLegacyMultiFileTexture() {
        val bytes = OutputBuffer(16).apply {
            writeShort(0x00AB)
            writeByte(0)
            writeByte(2)
            writeShort(10)
            writeShort(11)
            writeByte(4)
            writeByte(5)
            writeInt(0x111111)
            writeInt(0x222222)
            writeByte(6)
            writeByte(7)
        }.array()

        val texture = TextureLoader().setRev233(false).load(4, bytes)
        assertEquals(4, texture.id)
        assertEquals(0x00AB, texture.missingColor)
        assertFalse(texture.field1778)
        assertTrue(texture.fileIds.contentEquals(intArrayOf(10, 11)))
        assertTrue(texture.field1780.contentEquals(intArrayOf(4)))
        assertTrue(texture.field1781.contentEquals(intArrayOf(5)))
        assertTrue(texture.field1786.contentEquals(intArrayOf(0x111111, 0x222222)))
        assertEquals(6, texture.animationDirection)
        assertEquals(7, texture.animationSpeed)
    }
}
