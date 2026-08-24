package stan.qodat.scene.control.export.mp4

import stan.qodat.scene.runescape.animation.AnimationFrameLegacy
import stan.qodat.scene.runescape.animation.AnimationLegacy
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnimationToMp4TaskTest {

    @Test
    fun outputPathNestsMp4FolderAndUsesAnimationName() {
        val animation = AnimationLegacy("walk cycle")
        assertEquals("walk cycle", animation.getName())
        assertEquals(
            Path.of("/exports", "mp4", "walk cycle.mp4"),
            Path.of("/exports").resolve("mp4/${animation.getName()}.mp4")
        )
        assertEquals(
            Path.of("/exports", "mp4", ".mp4"),
            Path.of("/exports").resolve("mp4/${AnimationLegacy("").getName()}.mp4")
        )
        assertEquals(
            Path.of("/exports", "mp4", "a", "b.mp4"),
            Path.of("/exports").resolve("mp4/${AnimationLegacy("a/b").getName()}.mp4")
        )
    }

    @Test
    fun snapshotViewportRoundsOddExtentsDownByOne() {
        assertEquals(400.0, evenSnapshotExtent(400.0))
        assertEquals(400.0, evenSnapshotExtent(401.0))
        assertEquals(400.9, evenSnapshotExtent(401.9))
        assertEquals(0.0, evenSnapshotExtent(0.0))
        assertEquals(0.5, evenSnapshotExtent(0.5))
        assertEquals(-2.0, evenSnapshotExtent(-1.0))
    }

    @Test
    fun fpsUsesTheShortestFrameDuration() {
        val frames = listOf(
            AnimationFrameLegacy("slow", null, 5),
            AnimationFrameLegacy("fast", null, 1),
            AnimationFrameLegacy("mid", null, 2)
        )
        val minMillis = frames.minOf { it.getDuration() }.toMillis()
        val fps = 1000.0 / minMillis
        assertEquals(frames[1].getDuration().toMillis(), minMillis)
        assertTrue(fps > 0.0)
        assertEquals((1000.0 / minMillis).toInt(), fps.toInt())
    }

    @Test
    fun longerFramesRepeatUntilTheirDurationIsConsumed() {
        val shortFrame = AnimationFrameLegacy("short", null, 1)
        val longFrame = AnimationFrameLegacy("long", null, 5)
        val fps = 1000.0 / shortFrame.getDuration().toMillis()
        assertEquals(1, encodeRepeatCount(shortFrame.getDuration().toMillis(), fps))
        assertEquals(5, encodeRepeatCount(longFrame.getDuration().toMillis(), fps))
        assertEquals(0, encodeRepeatCount(0.0, fps))
    }

    /**
     * Mirrors `AnimationToMp4Task` viewport sizing:
     * `value.let { if (it.toInt() % 2 != 0) it - 1.0 else it }`.
     */
    private fun evenSnapshotExtent(value: Double): Double =
        if (value.toInt() % 2 != 0) value - 1.0 else value

    /**
     * Mirrors the `while (animationFrameDuration > 0)` encode loop in `AnimationToMp4Task`.
     */
    private fun encodeRepeatCount(animationFrameDurationMs: Double, fps: Double): Int {
        val videoFrameDurationInMs = 1000.0 / fps
        var remaining = animationFrameDurationMs
        var count = 0
        while (remaining > 0) {
            count++
            remaining -= videoFrameDurationInMs
        }
        return count
    }
}
