package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import qodat.cache.definition.ItemDefinition
import stan.qodat.cache.impl.oldschool.loader.ItemLoader226
import java.util.OptionalInt

class ItemManager(
    private val cacheLibrary: CacheLibrary
) {

    private val items = mutableMapOf<Int, ItemDefinition>()
    @Volatile
    private var loaded = false

    @Synchronized
    fun load() {
        if (loaded) return
        val archive = cacheLibrary.index(2).archive(10)!!
        val loader = ItemLoader226()
        // TODO(perf): decodes every item sequentially; archive is large and files are independent
        archive.files.forEach { (fileId, file) ->
            items[fileId] = loader.load(fileId, file.data ?: error("Item data null"))
        }
        loaded = true
    }

    fun getItems(): Array<ItemDefinition> {
        load()
        return getItems(items)
    }

    companion object {
        internal fun getItems(items: Map<Int, ItemDefinition>): Array<ItemDefinition> =
            items.values.toTypedArray()

        internal fun mapOldschoolItem(
            id: Int,
            name: String,
            inventoryModel: Int,
            colorFind: ShortArray?,
            colorReplace: ShortArray?,
        ): ItemDefinition = object : ItemDefinition {
            override fun getOptionalId() = OptionalInt.of(id)
            override val name = name
            override val modelIds = arrayOf(inventoryModel.toString())
            override val findColor = colorFind
            override val replaceColor = colorReplace
        }
    }
}
