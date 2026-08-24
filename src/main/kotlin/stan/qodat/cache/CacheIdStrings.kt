package stan.qodat.cache

import java.util.concurrent.ConcurrentHashMap

/**
 * Interns cache id strings so list wrap does not allocate a new
 * `"1234"` for every NPC / object / animation that shares a model.
 */
object CacheIdStrings {

    val EMPTY: Array<String> = emptyArray()

    private val intern = ConcurrentHashMap<Int, String>(512)

    fun of(id: Int): String = intern.getOrPut(id) { id.toString() }

    fun of(ids: IntArray): Array<String> {
        if (ids.isEmpty()) return EMPTY
        return Array(ids.size) { of(ids[it]) }
    }
}
