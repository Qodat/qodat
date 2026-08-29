package stan.qodat.scene.controller

import stan.qodat.scene.runescape.animation.AnimationLegacy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnimationControllerPoseTest {

    @Test
    fun nextPoseNameIncrements() {
        val controller = AnimationController()
        assertEquals("Pose", controller.nextPoseName())
        controller.animations.add(AnimationLegacy.createPoseClip("Pose"))
        assertEquals("Pose 2", controller.nextPoseName())
        controller.animations.add(AnimationLegacy.createPoseClip("Pose 2"))
        assertEquals("Pose 3", controller.nextPoseName())
    }

    @Test
    fun poseClipStartsWithOneEmptyFrame() {
        val clip = AnimationLegacy.createPoseClip("Pose")
        assertTrue(clip.isUserPoseClip())
        assertEquals(1, clip.getFrameList().size)
        val frame = clip.getFrameList().single() as stan.qodat.scene.runescape.animation.AnimationFrameLegacy
        assertEquals(0, frame.transformationList.size)
    }
}
