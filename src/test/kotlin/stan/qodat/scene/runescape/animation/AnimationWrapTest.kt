package stan.qodat.scene.runescape.animation

import qodat.cache.definition.AnimationDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnimationWrapTest {

    @Test
    fun catalogIdUsesDefinitionWithoutTouchingJavaFxIdProperty() {
        val definition = object : AnimationDefinition {
            override val id = "42"
            override val frameHashes = intArrayOf(1)
            override val frameLengths = intArrayOf(5)
            override val loopOffset = -1
            override val leftHandItem = -1
            override val rightHandItem = -1
        }
        val animation = AnimationLegacy(definition.id, definition, numericId = 42)
        assertEquals("42", animation.catalogId())
        assertEquals(42, animation.numericId())
        assertEquals("42", animation.getName())
    }

    @Test
    fun releaseDecodedFramesDropsLoadedListWithoutBreakingReload() {
        val animation = AnimationLegacy("empty", numericId = 1)
        val first = animation.getFrameList()
        assertTrue(first.isEmpty())
        animation.releaseDecodedFrames()
        val second = animation.getFrameList()
        assertTrue(second.isEmpty())
        assertTrue(first !== second)
    }
}
