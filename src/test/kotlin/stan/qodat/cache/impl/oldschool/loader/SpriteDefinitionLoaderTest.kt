package stan.qodat.cache.impl.oldschool.loader

import com.displee.io.impl.OutputBuffer
import net.runelite.cache.definitions.loaders.SpriteLoader as RuneLiteSpriteLoader
import stan.qodat.cache.impl.oldschool.definition.SpriteDefinition
import stan.qodat.cache.impl.oldschool.loader.SpriteLoader.Companion.FLAG_ALPHA
import stan.qodat.cache.impl.oldschool.loader.SpriteLoader.Companion.FLAG_VERTICAL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpriteDefinitionLoaderTest {

    @Test
    fun decodesSingleFrameSprite() {
        val rgb = 0x00AABB
        val sprites = SpriteLoader().load(44, singleFramePayload(rgb))
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
    }

    @Test
    fun verticalFlagReadsColumnMajorPixels() {
        val sprite = SpriteLoader().load(1, verticalPayload())[0]
        assertTrue(sprite.pixelIdx.contentEquals(byteArrayOf(1, 1, 2, 2)))
        assertEquals(2, sprite.width)
        assertEquals(2, sprite.height)
    }

    @Test
    fun alphaFlagKeepsTransparentIndexAndForcesOpaquePaletteHits() {
        val sprites = SpriteLoader().load(7, alphaPayload())
        assertEquals(1, sprites.size)
        val sprite = sprites[0]
        assertEquals(2, sprite.width)
        assertEquals(1, sprite.height)
        assertEquals(0, sprite.pixelIdx[0].toInt())
        assertEquals(1, sprite.pixelIdx[1].toInt())
        assertEquals(0x80000000.toInt(), sprite.pixels[0])
        assertEquals(0xFF112233.toInt(), sprite.pixels[1])
    }

    @Test
    fun decodesMultiFrameArchiveWithSharedPalette() {
        val sprites = SpriteLoader().load(9, multiFramePayload())
        assertEquals(2, sprites.size)

        val first = sprites[0]
        val second = sprites[1]
        assertEquals(9, first.id)
        assertEquals(0, first.frame)
        assertEquals(1, first.offsetX)
        assertEquals(2, first.offsetY)
        assertEquals(0xFFAA0000.toInt(), first.pixels[0])
        assertEquals(1, first.pixelIdx[0])

        assertEquals(9, second.id)
        assertEquals(1, second.frame)
        assertEquals(3, second.offsetX)
        assertEquals(4, second.offsetY)
        assertEquals(0xFF00BB00.toInt(), second.pixels[0])
        assertEquals(2, second.pixelIdx[0])

        assertEquals(2, first.maxWidth)
        assertEquals(2, first.maxHeight)
        assertTrue(first.palette.contentEquals(second.palette))
        assertEquals(0xAA0000, first.palette[1])
        assertEquals(0x00BB00, first.palette[2])
    }

    @Test
    fun remapsZeroPaletteEntriesToOne() {
        val sprite = SpriteLoader().load(3, singleFramePayload(0))[0]
        assertEquals(1, sprite.palette[1])
        assertEquals(0xFF000001.toInt(), sprite.pixels[0])
    }

    @Test
    fun newerDecoderReadsOlderHorizontalBytes() {
        val fromOldest = SpriteLoader().load(5, singleFramePayload(0x112233))
        val again = SpriteLoader().load(5, singleFramePayload(0x112233))
        assertEquals(fromOldest[0].pixels[0], again[0].pixels[0])
        assertEquals(0xFF112233.toInt(), again[0].pixels[0])
        assertEquals(1, again[0].pixelIdx[0])
    }

    @Test
    fun matchesRuneLiteOnFlagAndFrameVariants() {
        assertMatchesRuneLite(44, singleFramePayload(0x00AABB))
        assertMatchesRuneLite(1, verticalPayload())
        assertMatchesRuneLite(7, alphaPayload())
        assertMatchesRuneLite(9, multiFramePayload())
        assertMatchesRuneLite(3, singleFramePayload(0))
        assertMatchesRuneLite(8, verticalAlphaPayload())
    }

    private fun assertMatchesRuneLite(id: Int, bytes: ByteArray) {
        val rl = RuneLiteSpriteLoader().load(id, bytes)
        val ours = SpriteLoader().load(id, bytes)
        assertEquals(rl.size, ours.size)
        for (i in rl.indices) {
            assertSpriteEquals(rl[i], ours[i])
        }
    }

    private fun assertSpriteEquals(
        rl: net.runelite.cache.definitions.SpriteDefinition,
        ours: SpriteDefinition,
    ) {
        assertEquals(rl.id, ours.id)
        assertEquals(rl.frame, ours.frame)
        assertEquals(rl.offsetX, ours.offsetX)
        assertEquals(rl.offsetY, ours.offsetY)
        assertEquals(rl.width, ours.width)
        assertEquals(rl.height, ours.height)
        assertEquals(rl.maxWidth, ours.maxWidth)
        assertEquals(rl.maxHeight, ours.maxHeight)
        assertTrue(rl.pixels.contentEquals(ours.pixels))
        assertTrue(rl.pixelIdx.contentEquals(ours.pixelIdx))
        assertTrue(rl.palette.contentEquals(ours.palette))
    }

    private fun singleFramePayload(rgb: Int): ByteArray =
        OutputBuffer(16).apply {
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
        }.array()

    private fun verticalPayload(): ByteArray =
        OutputBuffer(16).apply {
            writeByte(FLAG_VERTICAL)
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
        }.array()

    private fun alphaPayload(): ByteArray =
        OutputBuffer(16).apply {
            writeByte(FLAG_ALPHA)
            writeByte(0)
            writeByte(1)
            writeByte(0x80)
            writeByte(0x40)
            write24BitInt(0x112233)
            writeShort(2)
            writeShort(1)
            writeByte(1)
            writeShort(0)
            writeShort(0)
            writeShort(2)
            writeShort(1)
            writeShort(1)
        }.array()

    private fun verticalAlphaPayload(): ByteArray =
        OutputBuffer(16).apply {
            writeByte(FLAG_VERTICAL or FLAG_ALPHA)
            writeByte(1)
            writeByte(0)
            writeByte(0x10)
            writeByte(0x20)
            write24BitInt(0x445566)
            writeShort(1)
            writeShort(2)
            writeByte(1)
            writeShort(0)
            writeShort(0)
            writeShort(1)
            writeShort(2)
            writeShort(1)
        }.array()

    private fun multiFramePayload(): ByteArray =
        OutputBuffer(16).apply {
            writeByte(0)
            writeByte(1)
            writeByte(0)
            writeByte(2)
            write24BitInt(0xAA0000)
            write24BitInt(0x00BB00)
            writeShort(2)
            writeShort(2)
            writeByte(2)
            writeShort(1)
            writeShort(3)
            writeShort(2)
            writeShort(4)
            writeShort(1)
            writeShort(1)
            writeShort(1)
            writeShort(1)
            writeShort(2)
        }.array()
}
