package stan.qodat.cache.impl.oldschool.loader

import com.displee.io.impl.OutputBuffer
import net.runelite.cache.definitions.SpriteDefinition as RuneLiteSpriteDefinition
import net.runelite.cache.definitions.loaders.TextureLoader as RuneLiteTextureLoader
import qodat.cache.definition.SpriteDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TextureDefinitionLoaderTest {

    @Test
    fun decodesRev233SingleFileTexture() {
        val texture = TextureLoader().load(9, rev233Payload())
        assertEquals(9, texture.id)
        assertTrue(texture.fileIds.contentEquals(intArrayOf(55)))
        assertEquals(0x1234, texture.missingColor)
        assertTrue(texture.field1778)
        assertEquals(2, texture.animationDirection)
        assertEquals(3, texture.animationSpeed)
        assertNull(texture.field1780)
        assertNull(texture.field1781)
        assertNull(texture.field1786)
    }

    @Test
    fun decodesLegacyMultiFileTexture() {
        val texture = TextureLoader().load(4, legacyMultiFilePayload())
        assertEquals(4, texture.id)
        assertEquals(0x00AB, texture.missingColor)
        assertFalse(texture.field1778)
        assertTrue(texture.fileIds.contentEquals(intArrayOf(10, 11)))
        assertTrue(texture.field1780!!.contentEquals(intArrayOf(4)))
        assertTrue(texture.field1781!!.contentEquals(intArrayOf(5)))
        assertTrue(texture.field1786!!.contentEquals(intArrayOf(0x111111, 0x222222)))
        assertEquals(6, texture.animationDirection)
        assertEquals(7, texture.animationSpeed)
    }

    @Test
    fun newerDecoderReadsLegacySingleFileTable() {
        val texture = TextureLoader().load(1, legacySingleFilePayload())
        assertEquals(1, texture.id)
        assertEquals(0x00CD, texture.missingColor)
        assertTrue(texture.field1778)
        assertTrue(texture.fileIds.contentEquals(intArrayOf(99)))
        assertNull(texture.field1780)
        assertNull(texture.field1781)
        assertTrue(texture.field1786!!.contentEquals(intArrayOf(0x333333)))
        assertEquals(1, texture.animationDirection)
        assertEquals(2, texture.animationSpeed)
    }

    @Test
    fun computePixelsSamplesNormalizedSpriteAtIdentitySize() {
        val texture = TextureLoader().load(9, rev233Payload())
        val sprite = testSprite(
            id = 55,
            width = 128,
            height = 128,
            maxWidth = 128,
            maxHeight = 128,
            pixelIdx = ByteArray(128 * 128) { 1 },
            palette = intArrayOf(0, 0x112233),
        )
        texture.computePixels(1.0, 128) { _, _ -> sprite }
        assertEquals(128 * 128, texture.pixels.size)
        assertTrue(texture.pixels.all { it == 0x112233 })
    }

    @Test
    fun computePixelsUpscales64SpriteTo128() {
        val texture = TextureLoader().load(9, rev233Payload())
        val pixelIdx = ByteArray(64 * 64) { i -> if (i == 0) 1 else 2 }
        val sprite = testSprite(
            id = 55,
            width = 64,
            height = 64,
            maxWidth = 64,
            maxHeight = 64,
            pixelIdx = pixelIdx,
            palette = intArrayOf(0, 0xAA0000, 0x00BB00),
        )
        texture.computePixels(1.0, 128) { _, _ -> sprite }
        assertEquals(0xAA0000, texture.pixels[0])
        assertEquals(0xAA0000, texture.pixels[1])
        assertEquals(0xAA0000, texture.pixels[128])
        assertEquals(0x00BB00, texture.pixels[2])
    }

    @Test
    fun computePixelsRequiresSprite() {
        val texture = TextureLoader().load(9, rev233Payload())
        assertFailsWith<IllegalArgumentException> {
            texture.computePixels(1.0, 128) { _, _ -> null }
        }
    }

    @Test
    fun matchesRuneLiteOnRev233AndLegacyPayloads() {
        assertMatchesRuneLite(9, rev233Payload())
        assertMatchesRuneLite(4, legacyMultiFilePayload(), rev233 = false)
        assertMatchesRuneLite(1, legacySingleFilePayload(), rev233 = false)
    }

    @Test
    fun computePixelsMatchesRuneLiteOnIdentityAndScaledSprites() {
        assertPixelsMatchRuneLite(9, rev233Payload(), spriteSize = 128, destSize = 128)
        assertPixelsMatchRuneLite(9, rev233Payload(), spriteSize = 64, destSize = 128)
        assertPixelsMatchRuneLite(9, rev233Payload(), spriteSize = 128, destSize = 64)
        assertPixelsMatchRuneLite(4, legacyMultiFilePayload(), spriteSize = 128, destSize = 128, rev233 = false)
    }

    private fun assertMatchesRuneLite(id: Int, bytes: ByteArray, rev233: Boolean = true) {
        val rl = RuneLiteTextureLoader().setRev233(rev233).load(id, bytes)
        val ours = TextureLoader().load(id, bytes)
        assertEquals(rl.id, ours.id)
        assertTrue(rl.fileIds.contentEquals(ours.fileIds))
        assertEquals(rl.missingColor, ours.missingColor)
        assertEquals(rl.field1778, ours.field1778)
        assertEquals(rl.animationDirection, ours.animationDirection)
        assertEquals(rl.animationSpeed, ours.animationSpeed)
        assertTrue(intArraysEqual(rl.field1780, ours.field1780))
        assertTrue(intArraysEqual(rl.field1781, ours.field1781))
        assertTrue(intArraysEqual(rl.field1786, ours.field1786))
    }

    private fun assertPixelsMatchRuneLite(
        id: Int,
        bytes: ByteArray,
        spriteSize: Int,
        destSize: Int,
        rev233: Boolean = true,
    ) {
        val palette = intArrayOf(0, 0x112233, 0x445566, 0x808080)
        val pixelIdx = ByteArray(spriteSize * spriteSize) { i -> ((i % 3) + 1).toByte() }
        val rlSprite = RuneLiteSpriteDefinition().apply {
            this.id = 55
            width = spriteSize
            height = spriteSize
            maxWidth = spriteSize
            maxHeight = spriteSize
            this.pixelIdx = pixelIdx.copyOf()
            this.palette = palette.copyOf()
            pixels = IntArray(0)
        }
        val oursSprite = testSprite(
            id = 55,
            width = spriteSize,
            height = spriteSize,
            maxWidth = spriteSize,
            maxHeight = spriteSize,
            pixelIdx = pixelIdx.copyOf(),
            palette = palette.copyOf(),
        )
        val rl = RuneLiteTextureLoader().setRev233(rev233).load(id, bytes)
        rl.method2680(1.0, destSize) { spriteId, _ ->
            if (spriteId == 55 || spriteId == 10 || spriteId == 11) rlSprite else null
        }
        val ours = TextureLoader().load(id, bytes)
        ours.computePixels(1.0, destSize) { spriteId, _ ->
            if (spriteId == 55 || spriteId == 10 || spriteId == 11) oursSprite else null
        }
        assertTrue(rl.pixels.contentEquals(ours.pixels))
    }

    private fun intArraysEqual(left: IntArray?, right: IntArray?): Boolean {
        if (left == null && right == null) return true
        if (left == null || right == null) return false
        return left.contentEquals(right)
    }

    private fun rev233Payload(): ByteArray =
        OutputBuffer(16).apply {
            writeShort(55)
            writeShort(0x1234)
            writeByte(1)
            writeByte(2)
            writeByte(3)
        }.array()

    private fun legacyMultiFilePayload(): ByteArray =
        OutputBuffer(16).apply {
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

    private fun legacySingleFilePayload(): ByteArray =
        OutputBuffer(16).apply {
            writeShort(0x00CD)
            writeByte(1)
            writeByte(1)
            writeShort(99)
            writeInt(0x333333)
            writeByte(1)
            writeByte(2)
        }.array()

    private fun testSprite(
        id: Int,
        width: Int,
        height: Int,
        maxWidth: Int,
        maxHeight: Int,
        pixelIdx: ByteArray,
        palette: IntArray,
        offsetX: Int = 0,
        offsetY: Int = 0,
    ): SpriteDefinition = object : SpriteDefinition {
        override val id = id
        override val frame = 0
        override val offsetX = offsetX
        override val offsetY = offsetY
        override val width = width
        override val height = height
        override val pixels = IntArray(0)
        override val maxWidth = maxWidth
        override val maxHeight = maxHeight
        override var pixelIdx = pixelIdx
        override var palette = palette
    }
}
