package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import qodat.cache.definition.NPCDefinition
import stan.qodat.Properties
import stan.qodat.cache.CacheIdStrings
import stan.qodat.cache.NpcPrimaryAnimations
import stan.qodat.cache.impl.oldschool.definition.NpcDefinition
import stan.qodat.cache.impl.oldschool.loader.NpcLoader
import java.io.File
import java.util.OptionalInt

class NpcManager(private val cacheLibrary: CacheLibrary) {

    val npcs = mutableMapOf<Int, NpcDefinition>()
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
            mapNpc(npc) { extraAnimationIds(npc, npcAnimsDir) }
        }.toTypedArray()
    }

    companion object {
        private val gson = GsonBuilder().create()
        private val intArrayType = object : TypeToken<IntArray>() {}.type

        internal fun mapNpc(
            npc: NpcDefinition,
            extraAnimationIds: Array<String>,
        ): NPCDefinition? = mapNpc(npc) { extraAnimationIds }

        internal fun mapNpc(
            npc: NpcDefinition,
            extraAnimationIds: () -> Array<String> = { emptyArray() },
        ): NPCDefinition? {
            val models = npc.models
            if (models == null || models.isEmpty()) return null
            return object : NPCDefinition {
                override fun getOptionalId() = OptionalInt.of(npc.id)
                override val name = npc.name.ifBlank { NpcDefinition.NULL_NAME }
                override val modelIds = CacheIdStrings.of(models)
                override val primaryAnimationIds by lazy { NpcPrimaryAnimations.ids(npc) }
                override val animationRoleLabels by lazy { NpcPrimaryAnimations.labels(npc) }
                override val animationIds by lazy {
                    val extra = extraAnimationIds()
                    if (extra.isEmpty()) primaryAnimationIds
                    else (primaryAnimationIds + extra).distinct().toTypedArray()
                }
                override val findColor = npc.recolorToFind
                override val replaceColor = npc.recolorToReplace
            }
        }

        internal fun extraAnimationIds(npc: NpcDefinition, npcAnimsDir: File): Array<String> {
            return try {
                val file = npcAnimsDir.resolve("${npc.id}.json")
                if (!file.isFile) return emptyArray()
                file.bufferedReader().use { gson.fromJson<IntArray>(it, intArrayType) }
                    ?.let { CacheIdStrings.of(it) }
                    ?: CacheIdStrings.EMPTY
            } catch (_: Exception) {
                emptyArray()
            }
        }
    }
}
