package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.runelite.cache.definitions.NpcDefinition
import net.runelite.cache.definitions.loaders.NpcLoader
import qodat.cache.definition.NPCDefinition
import stan.qodat.Properties
import stan.qodat.cache.NpcPrimaryAnimations
import java.io.File
import java.util.OptionalInt

class NpcManager(private val cacheLibrary: CacheLibrary) {

    val npcs = mutableMapOf<Int, NpcDefinition>()
    private val gson = GsonBuilder().create()
    private val intArrayType = object : TypeToken<IntArray>() {}.type
    @Volatile
    private var loaded = false

    @Synchronized
    fun load() {
        if (loaded) return
        val archive = cacheLibrary.index(2).archive(9) ?: error("Npc archive not found")
        val loader = NpcLoader().also { it.configureForRevision(archive.revision) }
        archive.files.forEach { (fileId, file) ->
            val data = file.data ?: return@forEach
            try {
                npcs[fileId] = loader.load(fileId, data)
            } catch (_: Exception) {
            }
        }
        loaded = true
    }

    fun getNpcs(): Array<NPCDefinition> {
        load()
        val npcAnimsDir = Properties.osrsCachePath.get().resolve("npc_anims").toFile()
        if (!npcAnimsDir.exists()) {
            npcAnimsDir.mkdirs()
        }
        return npcs.values.mapNotNull { npc ->
            if (npc.models == null || npc.models.isEmpty()) return@mapNotNull null
            object : NPCDefinition {
                override fun getOptionalId() = OptionalInt.of(npc.id)
                override val name = npc.name.ifBlank { "null" }
                override val modelIds = npc.models.map { it.toString() }.toTypedArray()
                override val primaryAnimationIds = NpcPrimaryAnimations.ids(npc)
                override val animationIds by lazy {
                    (primaryAnimationIds + extraAnimationIds(npc, npcAnimsDir))
                        .distinct()
                        .toTypedArray()
                }
                override val findColor = npc.recolorToFind
                override val replaceColor = npc.recolorToReplace
            }
        }.toTypedArray()
    }

    private fun extraAnimationIds(npc: NpcDefinition, npcAnimsDir: File): Array<String> {
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
