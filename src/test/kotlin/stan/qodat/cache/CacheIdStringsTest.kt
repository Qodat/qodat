package stan.qodat.cache

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CacheIdStringsTest {

    @Test
    fun internsRepeatedIdsAndEmptyArrays() {
        assertTrue(CacheIdStrings.of(42) === CacheIdStrings.of(42))
        assertEquals("42", CacheIdStrings.of(42))
        assertTrue(CacheIdStrings.of(intArrayOf()) === CacheIdStrings.EMPTY)
        val ids = CacheIdStrings.of(intArrayOf(1, 2, 1))
        assertTrue(ids.contentEquals(arrayOf("1", "2", "1")))
        assertTrue(ids[0] === ids[2])
    }
}
