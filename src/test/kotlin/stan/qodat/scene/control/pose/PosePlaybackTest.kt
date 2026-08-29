package stan.qodat.scene.control.pose

import stan.qodat.scene.runescape.animation.AnimationLegacy
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PosePlaybackTest {

    @Test
    fun bindPoseAndUserClipsAlwaysTakeTheLivePose() {
        assertTrue(PosePlayback.shouldApplyLivePose(null, playing = true))
        assertTrue(PosePlayback.shouldApplyLivePose(AnimationLegacy.createPoseClip("Pose"), playing = true))
        assertTrue(PosePlayback.shouldApplyLivePose(AnimationLegacy.createPoseClip("Pose"), playing = false))
    }

    @Test
    fun playingACacheSequenceDoesNotStackTheLivePose() {
        val cacheAnim = AnimationLegacy(
            "walk",
            definition = FakeAnimationDefinition(),
        )
        assertFalse(PosePlayback.shouldApplyLivePose(cacheAnim, playing = true))
        assertTrue(PosePlayback.shouldApplyLivePose(cacheAnim, playing = false))
    }
}

private class FakeAnimationDefinition : qodat.cache.definition.AnimationDefinition {
    override val id: String = "1"
    override val frameHashes: IntArray = intArrayOf()
    override val frameLengths: IntArray = intArrayOf()
    override val loopOffset: Int = -1
    override val leftHandItem: Int = -1
    override val rightHandItem: Int = -1
}
