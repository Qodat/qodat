package stan.qodat.cache.impl.oldschool.loader

import net.runelite.cache.definitions.loaders.SpriteLoader
import net.runelite.cache.io.OutputStream
import stan.qodat.cache.impl.oldschool.definition.RuneliteSpriteDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpriteDefinitionLoaderTest {

    @Test
    fun decodesSingleFrameSpriteAndWrapsDefinition() {
        val rgb = 0x00AABB
        val bytes = OutputStream().apply {
            writeByte(0)
            writeByte(1)
            write24BitInt(rgb)
            writeShort(1)
            writeShort(1)
            writeByte(1)
            writeShort(2)
            writeShort(3)
            writeShort(1)
            writeShort(1)
            writeShort(1)
        }.flip()

        val sprites = SpriteLoader().load(44, bytes)
        assertEquals(1, sprites.size)

        val sprite = sprites[0]
        assertEquals(44, sprite.id)
        assertEquals(0, sprite.frame)
        assertEquals(1, sprite.width)
        assertEquals(1, sprite.height)
        assertEquals(1, sprite.maxWidth)
        assertEquals(1, sprite.maxHeight)
        assertEquals(2, sprite.offsetX)
        assertEquals(3, sprite.offsetY)
        assertEquals(0xFF00AABB.toInt(), sprite.pixels[0])
        assertEquals(1, sprite.pixelIdx[0])
        assertEquals(rgb, sprite.palette[1])

        val mapped = RuneliteSpriteDefinition(sprite)
        assertEquals(44, mapped.id)
        assertEquals(0, mapped.frame)
        assertEquals(2, mapped.offsetX)
        assertEquals(3, mapped.offsetY)
        assertTrue(mapped.pixels.contentEquals(intArrayOf(0xFF00AABB.toInt())))
        assertTrue(mapped.pixelIdx.contentEquals(byteArrayOf(1)))
        assertEquals(rgb, mapped.palette[1])
    }

    @Test
    fun verticalFlagReadsColumnMajorPixels() {
        val bytes = OutputStream().apply {
            writeByte(SpriteLoader.FLAG_VERTICAL)
            writeByte(1)
            writeByte(2)
            writeByte(1)
            writeByte(2)
            write24BitInt(0x110000)
            write24BitInt(0x001100)
            writeShort(2)
            writeShort(2)
            writeByte(2)
            writeShort(0)
            writeShort(0)
            writeShort(2)
            writeShort(2)
            writeShort(1)
        }.flip()

        val sprite = SpriteLoader().load(1, bytes)[0]
        assertTrue(sprite.pixelIdx.contentEquals(byteArrayOf(1, 1, 2, 2)))
        assertEquals(2, sprite.width)
        assertEquals(2, sprite.height)
    }
}
