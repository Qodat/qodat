package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import net.runelite.cache.definitions.loaders.ObjectLoader
import qodat.cache.definition.ObjectDefinition
import java.util.OptionalInt

class ObjectManager(
    private val cacheLibrary: CacheLibrary
) {

    val objects = mutableMapOf<Int, ObjectDefinition>()

    fun load() {
        val loader = ObjectLoader()
        val archive = cacheLibrary.index(2).archive(6)?:error("Object archive not found")

        loader.configureForRevision(archive.revision)

        archive.files.forEach { (fileId, file) ->
            objects.put(fileId, convert(loader.load(fileId, file.data)))
        }
    }

    fun getObject(id: Int): ObjectDefinition {
        return objects[id]?:error("Object not found $id")
    }

    fun getObjects() =
        objects.values.toTypedArray()

    private fun convert(definition: net.runelite.cache.definitions.ObjectDefinition): qodat.cache.definition.ObjectDefinition =
        object : ObjectDefinition {
            override fun getOptionalId() = OptionalInt.of(definition.id)
            override val name = definition.name
            override val modelIds = definition.objectModels?.map { it.toString() }?.toTypedArray() ?: emptyArray()
            override val animationIds = if (definition.animationID == -1)
                emptyArray()
            else
                arrayOf(definition.animationID.toString())
            override val findColor = definition.recolorToFind
            override val replaceColor = definition.recolorToReplace
        }
}