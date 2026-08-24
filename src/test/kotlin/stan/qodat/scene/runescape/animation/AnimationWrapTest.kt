package stan.qodat.scene.runescape.animation

import qodat.cache.definition.AnimationDefinition
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
