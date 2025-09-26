package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import net.runelite.cache.definitions.NpcDefinition
import net.runelite.cache.definitions.loaders.NpcLoader
import qodat.cache.definition.NPCDefinition
import stan.qodat.Properties
import java.util.OptionalInt
import java.util.concurrent.ConcurrentHashMap

class NpcManager(private val cacheLibrary: CacheLibrary) {

    val npcs = mutableMapOf<Int, NpcDefinition>()
    private val gson = GsonBuilder().create()
    private val intArrayType = object: TypeToken<IntArray>() {}.type
    private val animIdsCache = ConcurrentHashMap<Int, Array<String>>()

    fun load() {


        val loader = NpcLoader()
        val archive = cacheLibrary.index(2).archive(9)?:error("Npc archive not found")

        loader.configureForRevision(archive.revision)

        archive.files.forEach { (fileId, file) ->
            npcs.put(fileId, loader.load(fileId, file.data))
        }
    }

    fun get(id: Int): NpcDefinition {
        return npcs[id]?:error("Npc not found $id")
    }

    fun getNpcs(): Array<NPCDefinition> {
        val npcAnimsDir = Properties.osrsCachePath.get().resolve("npc_anims").toFile()
        if (!npcAnimsDir.exists()){
            println("Did not find npc_anims dir, creating...")
            return emptyArray()
        }
        val animatedNpcs = runBlocking {
            npcs.values
                .filter { it.models != null && it.models.isNotEmpty() }
                .map { npc ->
                    async(Dispatchers.IO) {
                        object : NPCDefinition {
                            override fun getOptionalId() = OptionalInt.of(npc.id)
                            override val name = npc.name.ifBlank { "null" }
                            override val modelIds = npc.models.map { it.toString() }.toTypedArray()
                            override val animationIds = try {
                                animIdsCache.getOrPut(npc.standingAnimation) {
                                    val data = npcAnimsDir
                                        .resolve("${npc.id}.json")
                                        .bufferedReader()
                                        .use {gson.fromJson<IntArray>(it, intArrayType).map { it.toString() }.toTypedArray() }
                                    if (data.isEmpty()) {
                                        emptyArray()
                                    } else
                                        data
                                }
                            } catch (ignored: Exception) {
                                System.err.println("Failed to load anim data for npc ${npc.name} ${npc.standingAnimation}")
                                emptyArray()
                            }
                            override val findColor = npc.recolorToFind
                            override val replaceColor = npc.recolorToReplace
                        }
                    }
                }.awaitAll()
        }

        return animatedNpcs.toTypedArray()
    }
}