package stan.qodat.util

import javafx.util.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class FrameTimeUtilConversionTest {

    @Test
    fun frameDurationRoundTripsOutsideDefaultRange() {
        for (frame in intArrayOf(0, 1, 49, 100, 600, 3000)) {
            val duration = FrameTimeUtil.frame(frame)
            assertEquals(frame.toLong(), FrameTimeUtil.toFrame(duration))
            assertEquals(frame, FrameTimeUtil.toFrameAsInt(duration))
        }
    }

    @Test
    fun millisMapToNearestFrame() {
        assertEquals(0L, FrameTimeUtil.toFrame(Duration.ZERO))
        assertEquals(0, FrameTimeUtil.toFrameAsInt(Duration.ZERO))
        assertEquals(1, FrameTimeUtil.toFrameAsInt(Duration.millis(10.0)))
        assertEquals(1, FrameTimeUtil.toFrameAsInt(Duration.millis(20.0)))
        assertEquals(2, FrameTimeUtil.toFrameAsInt(Duration.millis(30.0)))
    }
}
