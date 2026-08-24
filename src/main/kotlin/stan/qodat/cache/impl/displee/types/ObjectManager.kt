package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import qodat.cache.definition.ObjectDefinition
import stan.qodat.cache.impl.oldschool.definition.ObjectDefinition as OsrsObjectDefinition
import stan.qodat.cache.impl.oldschool.loader.ObjectLoader

class ObjectManager(
    private val cacheLibrary: CacheLibrary
) {

    val objects = mutableMapOf<Int, OsrsObjectDefinition>()
    @Volatile
    private var loaded = false

    @Synchronized
    fun load() {
        if (loaded) return
        val archive = cacheLibrary.index(2).archive(6) ?: error("Object archive not found")
        val loader = ObjectLoader().also { it.configureForRevision(archive.revision) }
        archive.files.forEach { (fileId, file) ->
            val data = file.data ?: return@forEach
            objects[fileId] = loader.load(fileId, data)
        }
        loaded = true
    }

    fun getObjects(): Array<ObjectDefinition> {
        load()
        return objects.values.map(::mapObject).toTypedArray()
    }

    companion object {
        internal fun mapObject(definition: OsrsObjectDefinition): ObjectDefinition =
            object : ObjectDefinition {
                override fun getOptionalId() = definition.getOptionalId()
                override val name = definition.name
                override val modelIds = definition.modelIds
                override val animationIds = definition.animationIds
                override val findColor = definition.findColor
                override val replaceColor = definition.replaceColor
            }
    }
}
