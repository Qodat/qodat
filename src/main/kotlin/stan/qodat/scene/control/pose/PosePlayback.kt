package stan.qodat.scene.control.pose

import stan.qodat.scene.runescape.animation.Animation

/**
 * Live [ModelPose] is for posing a still / user clip. Stacking it on a
 * playing cache sequence fights that sequence's own vertex-group motion.
 */
object PosePlayback {

    fun shouldApplyLivePose(animation: Animation?, playing: Boolean): Boolean {
        if (animation == null || animation.isUserPoseClip())
            return true
        return !playing
    }
}
