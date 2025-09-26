package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import net.runelite.cache.definitions.loaders.ItemLoader
import qodat.cache.definition.ItemDefinition
import stan.qodat.cache.impl.oldschool.loader.ItemLoader226
import java.util.OptionalInt

class ItemManager(
    private val cacheLibrary: CacheLibrary
) {

    private val items = mutableMapOf<Int, ItemDefinition>()

    fun load() {
        val loader = ItemLoader226()
        val archive = cacheLibrary.index(2).archive(10)!!
        archive.files.forEach { (fileId, file) ->
            items.put(fileId, loader.load(fileId, file.data?:error("Item data null")))
        }
    }

    private fun convert(definition: net.runelite.cache.definitions.ItemDefinition): ItemDefinition {
        return object : ItemDefinition {
            override fun getOptionalId() = OptionalInt.of(definition.id)
            override val name = definition.name
            override val modelIds = arrayOf(definition.inventoryModel.toString())
            override val findColor = definition.colorFind
            override val replaceColor = definition.colorReplace
        }
    }

    fun getItems() = items.values.toTypedArray()

    fun getItem(id: Int) = items[id]?: throw IllegalArgumentException("Item $id not found")
}