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
        assertEquals(400.0, AnimationToMp4Task.evenDimension(400.0))
        assertEquals(400.0, AnimationToMp4Task.evenDimension(401.0))
        assertEquals(400.9, AnimationToMp4Task.evenDimension(401.9))
        assertEquals(0.0, AnimationToMp4Task.evenDimension(0.0))
        assertEquals(0.5, AnimationToMp4Task.evenDimension(0.5))
        assertEquals(-2.0, AnimationToMp4Task.evenDimension(-1.0))
    }

    @Test
    fun fpsUsesTheShortestFrameDuration() {
        val frames = listOf(
            AnimationFrameLegacy("slow", null, 5),
            AnimationFrameLegacy("fast", null, 1),
            AnimationFrameLegacy("mid", null, 2)
        )
        val fps = AnimationToMp4Task.fpsFromShortestFrame(frames)
        val shortestMillis = frames[1].getDuration().toMillis()
        assertEquals(1000.0 / shortestMillis, fps)
        assertTrue(fps > 0.0)
        assertEquals((1000.0 / shortestMillis).toInt(), fps.toInt())
    }

    @Test
    fun longerFramesRepeatUntilTheirDurationIsConsumed() {
        val shortFrame = AnimationFrameLegacy("short", null, 1)
        val longFrame = AnimationFrameLegacy("long", null, 5)
        val fps = AnimationToMp4Task.fpsFromShortestFrame(listOf(shortFrame))
        assertEquals(1, AnimationToMp4Task.encodeRepeatCount(shortFrame.getDuration().toMillis(), fps))
        assertEquals(5, AnimationToMp4Task.encodeRepeatCount(longFrame.getDuration().toMillis(), fps))
        assertEquals(0, AnimationToMp4Task.encodeRepeatCount(0.0, fps))
    }
}
