package stan.qodat.cache.impl.displee.types

import stan.qodat.cache.impl.displee.types.SpriteManager.Companion.getSprites
import stan.qodat.cache.impl.oldschool.definition.SpriteDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpriteManagerMappingTest {

    @Test
    fun spriteDefinitionExposesIdentityOffsetsAndBuffers() {
        val def = SpriteDefinition(id = 17, frame = 2).apply {
            offsetX = 3
            offsetY = 4
            width = 2
            height = 1
            maxWidth = 8
            maxHeight = 8
            pixels = intArrayOf(0xFF00FF00.toInt(), 0xFF0000FF.toInt())
            pixelIdx = byteArrayOf(1, 2)
            palette = intArrayOf(0, 0x00FF00, 0x0000FF)
        }

        assertEquals(17, def.id)
        assertEquals(2, def.frame)
        assertEquals(3, def.offsetX)
        assertEquals(4, def.offsetY)
        assertEquals(2, def.width)
        assertEquals(1, def.height)
        assertEquals(8, def.maxWidth)
        assertEquals(8, def.maxHeight)
        assertTrue(def.pixels.contentEquals(intArrayOf(0xFF00FF00.toInt(), 0xFF0000FF.toInt())))
        assertTrue(def.pixelIdx.contentEquals(byteArrayOf(1, 2)))
        assertTrue(def.palette.contentEquals(intArrayOf(0, 0x00FF00, 0x0000FF)))
    }

    @Test
    fun getSpritesReturnsLoadedDefinitions() {
        val sprite = SpriteDefinition(id = 4, frame = 1)
        val mapped = getSprites(listOf(sprite))
        assertEquals(1, mapped.size)
        assertEquals(sprite, mapped.single())
    }

    @Test
    fun getSpritesReturnsEmptyWhenNothingWasLoaded() {
        assertTrue(getSprites(emptyList()).isEmpty())
    }

    @Test
    fun indexEntriesAreOneStubPerArchiveId() {
        val stubs = SpriteManager.indexEntries(intArrayOf(0, 4, 12))
        assertEquals(listOf(0, 4, 12), stubs.map { it.id })
        assertTrue(stubs.all { it.frame == 0 && it.width == 0 && it.height == 0 })
    }
}
