package stan.qodat.cache.definition

import jagex.MayaAnimationSupport
import qodat.cache.definition.AnimationMayaDefinition
import stan.qodat.cache.impl.oldschool.definition.SequenceDefinition206
import stan.qodat.cache.impl.oldschool.definition.SequenceDefinition226
import stan.qodat.scene.runescape.animation.AnimationFrameMaya
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnimationMayaDefinitionFieldsTest {

    @Test
    fun sequence226MayaFieldsDefaultToUnset() {
        val def = SequenceDefinition226("4")
        assertEquals(-1, def.animMayaId)
        assertEquals(0, def.animMayaStart)
        assertEquals(0, def.animMayaEnd)
        assertNull(def.animMayaMasks)
    }

    @Test
    fun sequence206MayaFieldsDefaultToUnset() {
        val def = SequenceDefinition206("5")
        assertEquals(-1, def.animMayaId)
        assertEquals(0, def.animMayaStart)
        assertEquals(0, def.animMayaEnd)
    }

    @Test
    fun animationMayaDefinitionHoldsClipAndMaskFields() {
        val mapped = object : AnimationMayaDefinition {
            override val id = "12"
            override val frameHashes = intArrayOf(0x00010002)
            override val frameLengths = intArrayOf(3)
            override val loopOffset = 1
            override val leftHandItem = -1
            override val rightHandItem = -1
            override val animMayaID = 77
            override val animMayaFrameSounds = emptyMap<Int, net.runelite.cache.definitions.SequenceDefinition.Sound>()
            override val animMayaStart = 2
            override val animMayaEnd = 8
            override val animMayaMasks = booleanArrayOf(true, false, true)
        }
        assertEquals(77, mapped.animMayaID)
        assertEquals(2, mapped.animMayaStart)
        assertEquals(8, mapped.animMayaEnd)
        assertEquals(6, mayaClipFrameCount(mapped.animMayaStart, mapped.animMayaEnd, playbackLength = 1))
        assertTrue(mapped.animMayaMasks.contentEquals(booleanArrayOf(true, false, true)))
        assertTrue(mapped.animMayaFrameSounds.isEmpty())
    }

    @Test
    fun mayaClipUsesPlaybackLengthWhenRangeIsEmpty() {
        assertEquals(9, mayaClipFrameCount(4, 4, playbackLength = 9))
        assertEquals(9, mayaClipFrameCount(8, 3, playbackLength = 9))
        assertEquals(5, mayaClipFrameCount(0, 5, playbackLength = 99))
    }

    @Test
    fun animationFrameMayaReportsConfiguredLength() {
        MayaAnimationSupport.withStubIndex { index ->
            val anim = MayaAnimationSupport.load(
                index,
                MayaAnimationSupport.identitySkeleton(),
            )
            val frame = AnimationFrameMaya("frame[3]", duration = 7, index = 3, animation = anim)
            assertEquals(7, frame.getLength())
            assertEquals(3, frame.index)
            assertEquals(anim, frame.animation)
            assertEquals("frame[3]", frame.getName())
        }
    }

    /**
     * [stan.qodat.scene.runescape.animation.AnimationMaya.getFrameList] clip rule:
     * `end - start` when positive, otherwise [jagex.MayaAnimation.getPlaybackLength].
     */
    private fun mayaClipFrameCount(start: Int, end: Int, playbackLength: Int): Int {
        val clipLength = end - start
        return if (clipLength > 0) clipLength else playbackLength
    }
}
