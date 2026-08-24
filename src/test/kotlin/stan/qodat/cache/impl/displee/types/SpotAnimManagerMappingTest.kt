package stan.qodat.cache.impl.displee.types

import stan.qodat.cache.impl.displee.types.SpotAnimManager.Companion.getSpotAnimations
import stan.qodat.cache.impl.oldschool.definition.SpotAnimDefinition
import java.util.OptionalInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpotAnimManagerMappingTest {

    @Test
    fun spotAnimDefinitionExposesNumericNameAndSingleModel() {
        val def = SpotAnimDefinition(14).apply {
            modelId = 99
            animationId = 3
            recolorToFind = shortArrayOf(7)
            recolorToReplace = shortArrayOf(8)
        }

        assertEquals(OptionalInt.of(14), def.getOptionalId())
        assertEquals("14", def.name)
        assertTrue(def.modelIds.contentEquals(arrayOf("99")))
        assertTrue(def.animationIds.contentEquals(arrayOf("3")))
        assertTrue(def.findColor!!.contentEquals(shortArrayOf(7)))
        assertTrue(def.replaceColor!!.contentEquals(shortArrayOf(8)))
    }

    @Test
    fun getSpotAnimationsReturnsLoadedDefinitions() {
        val gfx = SpotAnimDefinition(14).apply { modelId = 99 }
        val mapped = getSpotAnimations(mapOf(14 to gfx))
        assertEquals(1, mapped.size)
        assertEquals(gfx, mapped.single())
    }

    @Test
    fun getSpotAnimationsReturnsEmptyWhenNothingWasLoaded() {
        assertTrue(getSpotAnimations(emptyMap()).isEmpty())
    }
}
