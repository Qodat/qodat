package stan.qodat.cache

import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/**
 * Sequential chunked wrap for cache list rows.
 *
 * Prefer this over per-item [kotlinx.coroutines.launch] or [java.util.stream.Stream.parallel]
 * on [kotlinx.coroutines.Dispatchers.Default]: those oversubscribe the common pool.
 */
object CacheListWrap {

    const val DEFAULT_CHUNK = 256

    inline fun <T, R : Any> mapNotNull(
        items: Array<T>,
        chunkSize: Int = DEFAULT_CHUNK,
        onChunk: () -> Unit = {},
        transform: (T) -> R?,
    ): List<R> = mapNotNullIndexed(items, chunkSize, onChunk) { _, item -> transform(item) }

    inline fun <T, R : Any> mapNotNullIndexed(
        items: Array<T>,
        chunkSize: Int = DEFAULT_CHUNK,
        onChunk: () -> Unit = {},
        transform: (index: Int, T) -> R?,
    ): List<R> {
        if (items.isEmpty()) return emptyList()
        val step = chunkSize.coerceAtLeast(1)
        val result = ArrayList<R>(items.size)
        var i = 0
        val size = items.size
        while (i < size) {
            onChunk()
            val end = (i + step).coerceAtMost(size)
            for (j in i until end) {
                transform(j, items[j])?.let { result.add(it) }
            }
            i = end
        }
        return result
    }

    suspend fun <T, R : Any> mapNotNullCancellable(
        items: Array<T>,
        chunkSize: Int = DEFAULT_CHUNK,
        transform: (T) -> R?,
    ): List<R> {
        val context = coroutineContext
        return mapNotNull(items, chunkSize, onChunk = { context.ensureActive() }, transform)
    }

    suspend fun <T, R : Any> mapNotNullIndexedCancellable(
        items: Array<T>,
        chunkSize: Int = DEFAULT_CHUNK,
        transform: (index: Int, T) -> R?,
    ): List<R> {
        val context = coroutineContext
        return mapNotNullIndexed(items, chunkSize, onChunk = { context.ensureActive() }, transform)
    }
}
