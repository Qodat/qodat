package stan.qodat.cache

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BoundedLruCacheTest {

    @Test
    fun evictsEldestWhenOverEntryCap() {
        val evicted = mutableListOf<Int>()
        val cache = BoundedLruCache<Int, String>(
            maxEntries = 2,
            onEvict = { key, _ -> evicted += key },
        )
        cache.put(1, "a")
        cache.put(2, "b")
        cache.put(3, "c")
        assertEquals(listOf(1), evicted)
        assertFalse(cache.contains(1))
        assertTrue(cache.contains(2))
        assertTrue(cache.contains(3))
    }

    @Test
    fun accessOrderKeepsRecentlyUsed() {
        val evicted = mutableListOf<Int>()
        val cache = BoundedLruCache<Int, String>(
            maxEntries = 2,
            onEvict = { key, _ -> evicted += key },
        )
        cache.put(1, "a")
        cache.put(2, "b")
        cache[1]
        cache.put(3, "c")
        assertEquals(listOf(2), evicted)
        assertTrue(cache.contains(1))
        assertTrue(cache.contains(3))
    }

    @Test
    fun evictsByWeightButKeepsNewest() {
        val evicted = mutableListOf<String>()
        val cache = BoundedLruCache<String, String>(
            maxEntries = 8,
            maxWeight = 10,
            weigher = { it.length.toLong() },
            onEvict = { key, _ -> evicted += key },
        )
        cache.put("a", "12345")
        cache.put("b", "12345")
        cache.put("c", "123456789012")
        assertTrue(evicted.contains("a"))
        assertTrue(cache.contains("c"))
        assertEquals(1, cache.size)
    }

    @Test
    fun getOrLoadCachesNullAndDoesNotReload() {
        var loads = 0
        val cache = BoundedLruCache<Int, String?>(maxEntries = 4)
        val first = cache.getOrLoad(7) {
            loads++
            null
        }
        val second = cache.getOrLoad(7) {
            loads++
            "nope"
        }
        assertNull(first)
        assertNull(second)
        assertEquals(1, loads)
    }

    @Test
    fun getOrLoadDeduplicatesAMissThenHit() {
        var loads = 0
        val cache = BoundedLruCache<Int, String>(maxEntries = 4)
        val first = cache.getOrLoad(1) {
            loads++
            "once"
        }
        val second = cache.getOrLoad(1) {
            loads++
            "twice"
        }
        assertEquals("once", first)
        assertEquals("once", second)
        assertEquals(1, loads)
    }
}
