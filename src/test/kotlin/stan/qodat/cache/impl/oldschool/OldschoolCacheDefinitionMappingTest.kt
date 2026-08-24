package stan.qodat.cache.impl.oldschool

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.runelite.cache.definitions.ObjectDefinition
import net.runelite.cache.definitions.SpotAnimDefinition
import qodat.cache.definition.NPCDefinition
import qodat.cache.definition.ObjectDefinition as QodatObjectDefinition
import qodat.cache.definition.SpotAnimationDefinition
import stan.qodat.cache.NpcPrimaryAnimations
import stan.qodat.cache.impl.oldschool.definition.NpcDefinition
import java.io.File
import java.util.OptionalInt
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OldschoolCacheDefinitionMappingTest {

    @Test
    fun npcMappingSkipsEmptyModelsAndUsesBlankNameSentinel() {
        assertNull(mapNpc(NpcDefinition(1)))
        val npc = NpcDefinition(2).apply {
            name = "  "
            models = intArrayOf(9)
            standingAnimation = 4
        }
        val mapped = mapNpc(npc)!!
        assertEquals("null", mapped.name)
        assertTrue(mapped.modelIds.contentEquals(arrayOf("9")))
        assertTrue(mapped.primaryAnimationIds.contentEquals(arrayOf("4")))
        assertTrue(mapped.animationIds.isEmpty())
    }

    @Test
    fun npcAnimationIdsAreExtrasOnlyUnlikeDispleeMerge() {
        val npc = NpcDefinition(5).apply {
            name = "Man"
            models = intArrayOf(1)
            standingAnimation = 10
        }
        val dir = createTempDirectory("osrs-npc-anims").toFile()
        try {
            dir.resolve("5.json").writeText("[10, 88]")
            val extras = extraAnimationIds(npc, dir)
            val mapped = mapNpc(npc, extras)
            assertTrue(mapped!!.primaryAnimationIds.contentEquals(arrayOf("10")))
            assertTrue(mapped.animationIds.contentEquals(arrayOf("10", "88")))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun extraAnimationIdsAreEmptyOnMissingOrCorruptFile() {
        val dir = createTempDirectory("osrs-npc-anims").toFile()
        try {
            assertTrue(extraAnimationIds(NpcDefinition(1), dir).isEmpty())
            dir.resolve("1.json").writeText("\"nope\"")
            assertTrue(extraAnimationIds(NpcDefinition(1), dir).isEmpty())
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun objectMappingUsesEmptyAnimationWhenSentinelIsMinusOne() {
        val withAnim = ObjectDefinition().apply {
            id = 1738
            name = "Door"
            objectModels = intArrayOf(50, 51)
            animationID = 88
            recolorToFind = shortArrayOf(1)
            recolorToReplace = shortArrayOf(2)
        }
        val mapped = mapObject(withAnim)
        assertEquals(OptionalInt.of(1738), mapped.getOptionalId())
        assertTrue(mapped.modelIds.contentEquals(arrayOf("50", "51")))
        assertTrue(mapped.animationIds.contentEquals(arrayOf("88")))

        val noAnim = ObjectDefinition().apply {
            id = 1
            name = "Rock"
            objectModels = null
            animationID = -1
        }
        val empty = mapObject(noAnim)
        assertTrue(empty.modelIds.isEmpty())
        assertTrue(empty.animationIds.isEmpty())
    }

    @Test
    fun spotAnimMappingUsesNumericNameAndSingleModel() {
        val spot = SpotAnimDefinition().apply {
            id = 14
            setModelId(99)
            animationId = 3
            recolorToFind = shortArrayOf(7)
            recolorToReplace = shortArrayOf(8)
        }
        val mapped = mapSpotAnim(spot)
        assertEquals(OptionalInt.of(14), mapped.getOptionalId())
        assertEquals("14", mapped.name)
        assertTrue(mapped.modelIds.contentEquals(arrayOf("99")))
        assertTrue(mapped.animationIds.contentEquals(arrayOf("3")))
        assertTrue(mapped.findColor!!.contentEquals(shortArrayOf(7)))
        assertTrue(mapped.replaceColor!!.contentEquals(shortArrayOf(8)))
    }

    companion object {
        private val gson = GsonBuilder().create()
        private val intArrayType = object : TypeToken<IntArray>() {}.type

        internal fun mapNpc(
            npc: NpcDefinition,
            extraAnimationIds: Array<String> = emptyArray(),
        ): NPCDefinition? {
            val models = npc.models
            if (models == null || models.isEmpty()) return null
            return object : NPCDefinition {
                override fun getOptionalId() = OptionalInt.of(npc.id)
                override val name = npc.name.ifBlank { "null" }
                override val modelIds = models.map { it.toString() }.toTypedArray()
                override val primaryAnimationIds = NpcPrimaryAnimations.ids(npc)
                override val animationIds = extraAnimationIds
                override val findColor = npc.recolorToFind
                override val replaceColor = npc.recolorToReplace
            }
        }

        internal fun extraAnimationIds(npc: NpcDefinition, npcAnimsDir: File): Array<String> {
            val file = npcAnimsDir.resolve("${npc.id}.json")
            if (!file.isFile) return emptyArray()
            return try {
                file.bufferedReader().use {
                    gson.fromJson<IntArray>(it, intArrayType)?.map { id -> id.toString() }?.toTypedArray()
                } ?: emptyArray()
            } catch (_: Exception) {
                emptyArray()
            }
        }

        internal fun mapObject(definition: ObjectDefinition): QodatObjectDefinition =
            object : QodatObjectDefinition {
                override fun getOptionalId() = OptionalInt.of(definition.id)
                override val name = definition.name
                override val modelIds = definition.objectModels?.map { it.toString() }?.toTypedArray()
                    ?: emptyArray()
                override val animationIds = if (definition.animationID == -1)
                    emptyArray()
                else
                    arrayOf(definition.animationID.toString())
                override val findColor = definition.recolorToFind
                override val replaceColor = definition.recolorToReplace
            }

        internal fun mapSpotAnim(spotAnim: SpotAnimDefinition): SpotAnimationDefinition =
            object : SpotAnimationDefinition {
                override fun getOptionalId() = OptionalInt.of(spotAnim.id)
                override val name: String = spotAnim.id.toString()
                override val modelIds: Array<String> = arrayOf(spotAnim.getModelId().toString())
                override val findColor: ShortArray? = spotAnim.recolorToFind
                override val replaceColor: ShortArray? = spotAnim.recolorToReplace
                override val animationIds: Array<String> = arrayOf(spotAnim.animationId.toString())
            }
    }
}
