package stan.qodat.cache.impl.displee.types

import stan.qodat.cache.impl.displee.types.TextureManager.Companion.getTextures
import stan.qodat.cache.impl.oldschool.definition.TextureDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextureManagerMappingTest {

    @Test
    fun textureDefinitionExposesFileIdsPixelsAndAnimation() {
        val def = TextureDefinition(9).apply {
            fileIds = intArrayOf(55)
            pixels = intArrayOf(1, 2, 3, 4)
            animationDirection = 2
            animationSpeed = 3
            missingColor = 0x1234
        }

        assertEquals(9, def.id)
        assertTrue(def.fileIds.contentEquals(intArrayOf(55)))
        assertTrue(def.pixels.contentEquals(intArrayOf(1, 2, 3, 4)))
        assertEquals(2, def.animationDirection)
        assertEquals(3, def.animationSpeed)
        assertEquals(0x1234, def.missingColor)
    }

    @Test
    fun getTexturesReturnsLoadedDefinitions() {
        val texture = TextureDefinition(9).apply { fileIds = intArrayOf(55) }
        val mapped = getTextures(mapOf(9 to texture))
        assertEquals(1, mapped.size)
        assertEquals(texture, mapped.single())
    }

    @Test
    fun getTexturesReturnsEmptyWhenNothingWasLoaded() {
        assertTrue(getTextures(emptyMap()).isEmpty())
    }
}
