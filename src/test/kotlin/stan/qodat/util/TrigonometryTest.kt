package stan.qodat.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrigonometryTest {

    @Test
    fun sineCosineMatchClientUnits() {
        assertEquals(0, SINE[0])
        assertEquals(65536, COSINE[0])
        assertEquals(65536, SINE[512])
        assertEquals(0, COSINE[512])
        assertTrue(SINE[256] in 46340..46341)
        assertEquals(SINE[1], -SINE[2047])
    }
}
