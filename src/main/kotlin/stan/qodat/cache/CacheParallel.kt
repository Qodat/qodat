package stan.qodat.cache

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.IntStream

/**
 * Parallel decode helpers. Callers must extract raw bytes first; do not share
 * [java.nio.ByteBuffer]s or call Displee [com.displee.cache.index.ReferenceTable.archive]
 * from these workers — the index is not safe for concurrent archive reads.
 */
object CacheParallel {

    const val SEQUENTIAL_THRESHOLD = 32

    val parallelism: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

    inline fun <V, T> decode(
        files: List<Pair<Int, V>>,
        crossinline decodeFile: (Int, V) -> T
    ): Map<Int, T> {
        if (files.isEmpty()) return emptyMap()
        if (files.size < SEQUENTIAL_THRESHOLD) {
            val result = LinkedHashMap<Int, T>(files.size)
            for ((id, data) in files) {
                result[id] = decodeFile(id, data)
            }
            return result
        }
        val result = ConcurrentHashMap<Int, T>(files.size)
        files.parallelStream().forEach { (id, data) ->
            result[id] = decodeFile(id, data)
        }
        return result
    }

    inline fun <T> forEachIndexed(
        size: Int,
        crossinline action: (Int) -> T
    ) {
        if (size < SEQUENTIAL_THRESHOLD) {
            for (i in 0 until size) action(i)
            return
        }
        IntStream.range(0, size).parallel().forEach { action(it) }
    }

    fun nextProgressStep(counter: AtomicInteger, total: Int, updateFrequency: Int): Int? {
        val count = counter.incrementAndGet()
        return if (count % updateFrequency == 0 || count == total) count else null
    }
}
