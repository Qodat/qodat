package jagex

import jagex.MayaAnimationSupport.FLAG_CHANNEL_0
import jagex.MayaAnimationSupport.FLAG_CHANNEL_TRANSLATE_X
import jagex.MayaAnimationSupport.TYPE_SECONDARY
import jagex.MayaAnimationSupport.TYPE_TRANSFORMATION
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MayaAnimationLoadAndEvaluateTest {

    @Test
    fun linearCurveEvaluatesToKeyframeValue() {
        val frame = MayaAnimationSupport.linearCurve(0.25f, frameNumber = 3)
        assertEquals(0.25f, frame.evaluate(3))
        // DEFAULT state holds the first/last key outside the baked window
        // (NR RSMayaFrames.method4555 when field1916 == 0).
        assertEquals(0.25f, frame.evaluate(2))
        assertEquals(0.25f, frame.evaluate(4))
        assertEquals(3, frame.lastFrame)
    }

    @Test
    fun skeletonDecodesMayaBonesAndType5Labels() {
        val skeleton = Skeleton(9, MayaAnimationSupport.identitySkeleton())
        assertEquals(9, skeleton.id)
        assertEquals(1, skeleton.count)
        assertTrue(skeleton.transformTypes.contentEquals(intArrayOf(5)))
        assertTrue(skeleton.labels[0].contentEquals(intArrayOf(0)))
        val maya = skeleton.mayaAnimationSkeleton
        assertNotNull(maya)
        assertEquals(1, maya.boneCount)
        assertNotNull(maya.getBone(0))
        assertNull(maya.getBone(1))
    }

    @Test
    fun skeletonWithoutTrailingBoneTableHasNoMayaSkeleton() {
        val skeleton = Skeleton(2, MayaAnimationSupport.skeletonWithoutMayaBones())
        assertNull(skeleton.mayaAnimationSkeleton)
        assertEquals(1, skeleton.count)
    }

    @Test
    fun loadPopulatesDurationPlaybackAndTransformationFlag() {
        MayaAnimationSupport.withStubIndex { index ->
            val anim = MayaAnimationSupport.load(
                index,
                MayaAnimationSupport.identitySkeleton(),
                MayaAnimationSupport.animationBytes(
                    totalDuration = 2,
                    curves = listOf(
                        MayaAnimationSupport.constantCurve(
                            value = 0.5f,
                            frameNumber = 6,
                            type = TYPE_TRANSFORMATION,
                            trackIndex = 0,
                            flag = FLAG_CHANNEL_0,
                        ),
                        MayaAnimationSupport.constantCurve(
                            value = 10f,
                            type = TYPE_SECONDARY,
                            trackIndex = 0,
                            flag = FLAG_CHANNEL_TRANSLATE_X,
                        ),
                    ),
                ),
            )
            assertEquals(2, anim.duration)
            assertTrue(anim.hasTransformations())
            assertEquals(6, anim.playbackLength)
            assertEquals(0.5f, anim.primaryFrames[0][0].evaluate(6))
            assertEquals(10f, anim.secondaryFrames[0][3].evaluate(0))
        }
    }

    @Test
    fun loadWithoutCurvesKeepsPlaybackLengthAtLeastOne() {
        MayaAnimationSupport.withStubIndex { index ->
            val anim = MayaAnimationSupport.load(
                index,
                MayaAnimationSupport.identitySkeleton(),
                MayaAnimationSupport.animationBytes(totalDuration = 0),
            )
            assertEquals(0, anim.duration)
            assertFalse(anim.hasTransformations())
            assertEquals(1, anim.playbackLength)
        }
    }

    @Test
    fun loadReturnsNullWhenSkeletonFileHasNoData() {
        MayaAnimationSupport.withStubIndex { index ->
            MayaAnimationSupport.pack(index, 0, 0, MayaAnimationSupport.animationBytes())
            MayaAnimationSupport.pack(index, MayaAnimationSupport.FRAME_GROUP_ID, 0, null)
            assertNull(MayaAnimation.load(index, index, 0, false))
        }
    }
}
