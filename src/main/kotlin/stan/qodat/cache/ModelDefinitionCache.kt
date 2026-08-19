package stan.qodat.cache

import qodat.cache.definition.ModelDefinition

/**
 * LRU of decoded model definitions. Humanoid NPCs reuse the same body models,
 * so arrow-key browse should not re-read and re-decode those archives.
 */
object ModelDefinitionCache {

    private const val MAX_ENTRIES = 256

    private val map = object : LinkedHashMap<String, ModelDefinition>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ModelDefinition>): Boolean =
            size > MAX_ENTRIES
    }

    fun getOrLoad(cacheName: String, id: String, load: () -> ModelDefinition): ModelDefinition {
        val key = "$cacheName:$id"
        synchronized(map) {
            map[key]?.let { return it }
        }
        val loaded = load()
        synchronized(map) {
            return map.getOrPut(key) { loaded }
        }
    }

    fun clear() {
        synchronized(map) { map.clear() }
    }
}
