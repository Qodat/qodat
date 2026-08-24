package stan.qodat.cache

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CacheListWrapTest {

    @Test
    fun mapNotNullKeepsNonNullInOrder() {
        val items = arrayOf(1, 2, 3, 4, 5)
        val mapped = CacheListWrap.mapNotNull(items, chunkSize = 2) { value ->
            if (value % 2 == 0) value * 10 else null
        }
        assertEquals(listOf(20, 40), mapped)
    }

    @Test
    fun mapNotNullIndexedPassesOriginalIndex() {
        val items = arrayOf("a", "b", "c")
        val mapped = CacheListWrap.mapNotNullIndexed(items) { index, value ->
            "$index:$value"
        }
        assertEquals(listOf("0:a", "1:b", "2:c"), mapped)
    }

    @Test
    fun mapNotNullReturnsEmptyForEmptyInput() {
        assertEquals(emptyList(), CacheListWrap.mapNotNull(emptyArray<Int>()) { it })
    }

    @Test
    fun mapNotNullInvokesOnChunkAtBoundaries() {
        val chunks = AtomicInteger()
        CacheListWrap.mapNotNull(
            items = Array(10) { it },
            chunkSize = 4,
            onChunk = { chunks.incrementAndGet() },
        ) { it }
        assertEquals(3, chunks.get())
    }

    @Test
    fun mapNotNullHonorsOnChunkCancellation() {
        val started = AtomicInteger()
        assertFailsWith<CancellationException> {
            CacheListWrap.mapNotNull(
                items = Array(1_000) { it },
                chunkSize = 10,
                onChunk = {
                    if (started.incrementAndGet() > 2) throw CancellationException()
                },
            ) { it }
        }
        assertTrue(started.get() in 3..4)
    }

    @Test
    fun mapNotNullCancellableHonorsCancelledContext() = runBlocking {
        val job = launch(Dispatchers.Unconfined) {
            cancel()
            CacheListWrap.mapNotNullCancellable(Array(32) { it }, chunkSize = 8) { it }
        }
        job.join()
        assertTrue(job.isCancelled)
    }
}
