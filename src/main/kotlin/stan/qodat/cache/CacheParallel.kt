package stan.qodat.cache

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.IntStream

/**
 * Parallel decode helpers. Callers must extract raw bytes first; do not share
 * [java.nio.ByteBuffer]s or call Displee [com.displee.cache.index.ReferenceTable.archive]
 * from these workers — the index is not safe for concurrent archive reads.
 *
 * Large batches run on [Dispatchers.Default] (same pool as
 * [stan.qodat.Qodat.applicationScope]), not [java.util.concurrent.ForkJoinPool.commonPool].
 */
object CacheParallel {

    const val SEQUENTIAL_THRESHOLD = 32

    val parallelism: Int = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

    val decodeExecutor: Executor = Dispatchers.Default.asExecutor()

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
        val futures = Array(files.size) { index ->
            val (id, data) = files[index]
            CompletableFuture.supplyAsync({ id to decodeFile(id, data) }, decodeExecutor)
        }
        val result = LinkedHashMap<Int, T>(files.size)
        for (future in futures) {
            val (id, value) = future.join()
            result[id] = value
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
