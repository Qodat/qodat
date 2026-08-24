package stan.qodat.cache

/**
 * Access-order LRU with a count cap and an optional estimated-byte budget.
 *
 * The newest insert is always kept, even when it alone exceeds [maxWeight],
 * so a single large preview cannot empty the cache.
 */
class BoundedLruCache<K, V>(
    val maxEntries: Int,
    val maxWeight: Long = Long.MAX_VALUE,
    private val weigher: (V) -> Long = { 1L },
    private val onEvict: (K, V) -> Unit = { _, _ -> },
) {

    private val map = LinkedHashMap<K, V>(16, 0.75f, true)
    private var totalWeight = 0L

    val size: Int
        get() = synchronized(this) { map.size }

    val weight: Long
        get() = synchronized(this) { totalWeight }

    fun contains(key: K): Boolean = synchronized(this) { map.containsKey(key) }

    operator fun get(key: K): V? = synchronized(this) { map[key] }

    fun getOrLoad(key: K, load: () -> V): V {
        synchronized(this) {
            if (map.containsKey(key)) return map.getValue(key)
        }
        val loaded = load()
        synchronized(this) {
            if (map.containsKey(key)) return map.getValue(key)
            putUnlocked(key, loaded)
            return loaded
        }
    }

    fun put(key: K, value: V) {
        synchronized(this) {
            putUnlocked(key, value)
        }
    }

    fun remove(key: K): V? = synchronized(this) {
        val previous = map.remove(key) ?: return null
        totalWeight -= weigher(previous).coerceAtLeast(0L)
        previous
    }

    fun clear() {
        synchronized(this) {
            map.clear()
            totalWeight = 0L
        }
    }

    private fun putUnlocked(key: K, value: V) {
        val previous = map.remove(key)
        if (previous != null)
            totalWeight -= weigher(previous).coerceAtLeast(0L)
        map[key] = value
        totalWeight += weigher(value).coerceAtLeast(0L)
        evictOverflow()
    }

    private fun evictOverflow() {
        while (map.size > 1 && (map.size > maxEntries || totalWeight > maxWeight)) {
            val eldest = map.entries.first()
            map.remove(eldest.key)
            totalWeight -= weigher(eldest.value).coerceAtLeast(0L)
            onEvict(eldest.key, eldest.value)
        }
    }
}
