package stan.qodat.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FrameRateMeasurerTest {

    @Test
    fun measureStaysEmptyUntilTheRingBufferFills() {
        val measurer = FrameRateMeasurer()
        repeat(99) { index ->
            assertTrue(measurer.measure(index.toLong()).isEmpty)
        }
        assertTrue(measurer.measure(99).isPresent)
    }

    @Test
    fun filledWindowReportsTheReciprocalOfTheSamplePeriod() {
        val measurer = FrameRateMeasurer()
        val periodNanos = 16_666_666L
        var now = 0L
        repeat(200) {
            measurer.measure(now)
            now += periodNanos
        }
        assertEquals(60, measurer.measure(now).asInt)
    }
}
