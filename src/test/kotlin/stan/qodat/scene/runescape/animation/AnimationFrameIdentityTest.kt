package stan.qodat.scene.runescape.animation

import qodat.cache.definition.AnimationFrameLegacyDefinition
import qodat.cache.definition.AnimationTransformationGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AnimationFrameIdentityTest {

    @Test
    fun splitsFrameHashHexIntoFileAndFrameIds() {
        val frame = AnimationFrameLegacy("frame", null, 1)
        val hash = (0x001A shl 16) or 0x000B
        val hex = Integer.toHexString(hash)
        assertEquals("1a000b", hex)
        assertEquals(0x1A, frame.getFileId(hex))
        assertEquals(0x0B, frame.getFrameId(hex))
    }

    @Test
    fun splitsShortHexWhenHighWordIsSmall() {
        val frame = AnimationFrameLegacy("frame", null, 1)
        val hex = Integer.toHexString((1 shl 16) or 2)
        assertEquals("10002", hex)
        assertEquals(1, frame.getFileId(hex))
        assertEquals(2, frame.getFrameId(hex))
    }

    @Test
    fun hexShorterThanFiveDigitsCannotYieldAFileId() {
        val frame = AnimationFrameLegacy("frame", null, 1)
        assertFailsWith<StringIndexOutOfBoundsException> {
            frame.getFileId(Integer.toHexString(0xABC))
        }
        assertFailsWith<NumberFormatException> {
            frame.getFileId(Integer.toHexString(0xABCD))
        }
    }

    @Test
    fun cloneKeepsFileIdAndIncrementsFrameId() {
        val frame = AnimationFrameLegacy("frame", emptyFrameDefinition(), 1).apply {
            idProperty.set((0x001A shl 16) or 0x000B)
        }
        val copy = frame.clone("copy")
        val hex = Integer.toHexString(copy.idProperty.get())
        assertEquals(0x1A, copy.getFileId(hex))
        assertEquals(0x0C, copy.getFrameId(hex))
        assertEquals("copy", copy.getName())
    }

    private fun emptyFrameDefinition(): AnimationFrameLegacyDefinition =
        object : AnimationFrameLegacyDefinition {
            override val transformationCount = 0
            override val transformationGroupAccessIndices = intArrayOf()
            override val transformationDeltaX = intArrayOf()
            override val transformationDeltaY = intArrayOf()
            override val transformationDeltaZ = intArrayOf()
            override val transformationGroup = object : AnimationTransformationGroup {
                override val id = 1
                override val transformationTypes = intArrayOf()
                override val targetVertexGroupsIndices = emptyArray<IntArray>()
            }
        }
}
