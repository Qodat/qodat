package stan.qodat.cache

import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CacheParallelTest {

    @Test
    fun sequentialDecodeKeepsInputOrder() {
        val files = List(8) { it to it * 10 }
        val decoded = CacheParallel.decode(files) { id, value -> id to value }
        assertEquals((0 until 8).toList(), decoded.keys.toList())
        assertEquals(70, decoded.getValue(7).second)
    }

    @Test
    fun parallelDecodeUsesDefaultPoolNotCommonPool() {
        val files = List(CacheParallel.SEQUENTIAL_THRESHOLD + 4) { it to it }
        val pools = ConcurrentHashMap.newKeySet<String>()
        val decoded = CacheParallel.decode(files) { id, value ->
            pools.add(Thread.currentThread().name)
            id + value
        }
        assertEquals(files.size, decoded.size)
        assertEquals(6, decoded.getValue(3))
        assertFalse(pools.any { it.startsWith("ForkJoinPool.commonPool") })
        assertTrue(pools.isNotEmpty())
    }
}
