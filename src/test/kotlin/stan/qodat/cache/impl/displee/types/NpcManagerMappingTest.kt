package stan.qodat.cache.impl.displee.types

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.runelite.cache.definitions.NpcDefinition
import qodat.cache.definition.NPCDefinition
import stan.qodat.cache.NpcPrimaryAnimations
import java.io.File
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

    companion object {
        private val gson = GsonBuilder().create()
        private val intArrayType = object : TypeToken<IntArray>() {}.type

        internal fun mapNpc(
            npc: NpcDefinition,
            extraAnimationIds: Array<String> = emptyArray(),
        ): NPCDefinition? {
            if (npc.models == null || npc.models.isEmpty()) return null
            return object : NPCDefinition {
                override fun getOptionalId() = OptionalInt.of(npc.id)
                override val name = npc.name.ifBlank { "null" }
                override val modelIds = npc.models.map { it.toString() }.toTypedArray()
                override val primaryAnimationIds = NpcPrimaryAnimations.ids(npc)
                override val animationIds =
                    (primaryAnimationIds + extraAnimationIds).distinct().toTypedArray()
                override val findColor = npc.recolorToFind
                override val replaceColor = npc.recolorToReplace
            }
        }

        internal fun extraAnimationIds(npc: NpcDefinition, npcAnimsDir: File): Array<String> {
            return try {
                val file = npcAnimsDir.resolve("${npc.id}.json")
                if (!file.isFile) return emptyArray()
                file.bufferedReader().use { gson.fromJson<IntArray>(it, intArrayType) }
                    ?.map { it.toString() }
                    ?.toTypedArray()
                    ?: emptyArray()
            } catch (_: Exception) {
                emptyArray()
            }
        }
    }
}
