package stan.qodat.cache.impl.displee.types

import stan.qodat.cache.impl.oldschool.definition.NpcDefinition
import stan.qodat.cache.impl.displee.types.NpcManager.Companion.extraAnimationIds
import stan.qodat.cache.impl.displee.types.NpcManager.Companion.mapNpc
import java.util.OptionalInt
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NpcManagerMappingTest {

    @Test
    fun skipsNpcsWithoutModels() {
        assertNull(mapNpc(NpcDefinition(1).apply { name = "Ghost" }))
        assertNull(mapNpc(NpcDefinition(2).apply {
            name = "Empty"
            models = intArrayOf()
        }))
    }

    @Test
    fun mapsModelsRecolorsAndBlankName() {
        val npc = NpcDefinition(394).apply {
            name = ""
            models = intArrayOf(100, 101)
            recolorToFind = shortArrayOf(0x1111)
            recolorToReplace = shortArrayOf(0x2222)
            standingAnimation = 20
            walkingAnimation = 21
        }

        val mapped = mapNpc(npc)!!
        assertEquals(OptionalInt.of(394), mapped.getOptionalId())
        assertEquals("null", mapped.name)
        assertTrue(mapped.modelIds.contentEquals(arrayOf("100", "101")))
        assertTrue(mapped.findColor.contentEquals(shortArrayOf(0x1111)))
        assertTrue(mapped.replaceColor.contentEquals(shortArrayOf(0x2222)))
        assertTrue(mapped.primaryAnimationIds.contentEquals(arrayOf("20", "21")))
        assertTrue(mapped.animationIds.contentEquals(arrayOf("20", "21")))
    }

    @Test
    fun mergesPrimaryAndExtraAnimationIdsDistinctly() {
        val npc = NpcDefinition(7).apply {
            name = "Guard"
            models = intArrayOf(1)
            standingAnimation = 10
            walkingAnimation = 11
        }
        val mapped = mapNpc(npc, extraAnimationIds = arrayOf("11", "99"))!!
        assertTrue(mapped.primaryAnimationIds.contentEquals(arrayOf("10", "11")))
        assertTrue(mapped.animationIds.contentEquals(arrayOf("10", "11", "99")))
    }

    @Test
    fun extraAnimationIdsAreEmptyWhenFileIsMissing() {
        val dir = createTempDirectory("npc-anims").toFile()
        try {
            assertTrue(extraAnimationIds(NpcDefinition(3), dir).isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun extraAnimationIdsReadJsonIntsAsStrings() {
        val dir = createTempDirectory("npc-anims").toFile()
        try {
            dir.resolve("8.json").writeText("[12, 13, 12]")
            assertTrue(extraAnimationIds(NpcDefinition(8), dir).contentEquals(arrayOf("12", "13", "12")))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun extraAnimationIdsAreEmptyOnInvalidJson() {
        val dir = createTempDirectory("npc-anims").toFile()
        try {
            dir.resolve("9.json").writeText("{not-an-array}")
            assertTrue(extraAnimationIds(NpcDefinition(9), dir).isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }
}
