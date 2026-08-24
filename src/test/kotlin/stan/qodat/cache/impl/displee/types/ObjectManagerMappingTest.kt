package stan.qodat.cache.impl.displee.types

import stan.qodat.cache.impl.displee.types.ObjectManager.Companion.mapObject
import stan.qodat.cache.impl.oldschool.definition.ObjectDefinition
import java.util.OptionalInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObjectManagerMappingTest {

    @Test
    fun mapsModelsRecolorsAndAnimation() {
        val obj = ObjectDefinition(1738).apply {
            name = "Door"
            objectModels = intArrayOf(50, 51)
            animationId = 88
            recolorToFind = shortArrayOf(0x0102)
            recolorToReplace = shortArrayOf(0x0304)
        }

        val mapped = mapObject(obj)
        assertEquals(OptionalInt.of(1738), mapped.getOptionalId())
        assertEquals("Door", mapped.name)
        assertTrue(mapped.modelIds.contentEquals(arrayOf("50", "51")))
        assertTrue(mapped.animationIds.contentEquals(arrayOf("88")))
        assertTrue(mapped.findColor.contentEquals(shortArrayOf(0x0102)))
        assertTrue(mapped.replaceColor.contentEquals(shortArrayOf(0x0304)))
    }

    @Test
    fun minusOneAnimationBecomesEmptyIds() {
        val mapped = mapObject(ObjectDefinition(1).apply {
            name = "Rock"
            objectModels = null
            animationId = -1
        })
        assertTrue(mapped.modelIds.isEmpty())
        assertTrue(mapped.animationIds.isEmpty())
    }
}
