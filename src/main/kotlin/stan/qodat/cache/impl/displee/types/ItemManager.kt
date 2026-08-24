package stan.qodat.cache.impl.displee.types

import com.displee.cache.CacheLibrary
import qodat.cache.definition.ItemDefinition
import stan.qodat.cache.impl.oldschool.loader.ItemLoader226

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
        return items.values.toTypedArray()
    }

    fun getItem(id: Int): ItemDefinition {
        load()
        return items[id] ?: throw IllegalArgumentException("Item $id not found")
    }
}
