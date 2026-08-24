package stan.qodat.util

import kotlin.test.Test
import kotlin.test.assertEquals

class FrameTimeUtilTest {

    @Test
    fun frameDurationRoundTrips() {
        for (frame in 0..48) {
            val duration = FrameTimeUtil.frame(frame)
            assertEquals(frame.toLong(), FrameTimeUtil.toFrame(duration))
            assertEquals(frame, FrameTimeUtil.toFrameAsInt(duration))
        }
    }
}
