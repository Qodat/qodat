package stan.qodat.scene.runescape.ui

import qodat.cache.definition.SpriteDefinition
import kotlin.test.Test
import kotlin.test.assertEquals

class SpriteSceneBuilderTest {

    @Test
    fun archiveFramesKeepsSameIdSortedAndDropsEmpty() {
        val selected = fake(id = 9, frame = 2, width = 4, height = 4)
        val frames = SpriteSceneBuilder.archiveFrames(
            selected,
            listOf(
                fake(id = 9, frame = 2, width = 4, height = 4),
                fake(id = 9, frame = 0, width = 4, height = 4),
                fake(id = 8, frame = 0, width = 4, height = 4),
                fake(id = 9, frame = 1, width = 0, height = 0),
            ),
        )
        assertEquals(listOf(0, 2), frames.map { it.frame })
    }

    @Test
    fun spritePassesArchiveFramesThroughToTheScene() {
        val frame0 = fake(id = 3, frame = 0, width = 2, height = 2)
        val frame1 = fake(id = 3, frame = 1, width = 2, height = 2)
        val sprite = Sprite(frame1, listOf(frame1, frame0))
        assertEquals(listOf(0, 1), sprite.archiveFrames.map { it.frame })
        assertEquals("3[1]", sprite.getName())
    }

    private fun fake(id: Int, frame: Int, width: Int, height: Int) = object : SpriteDefinition {
        override val id = id
        override val frame = frame
        override val offsetX = 0
        override val offsetY = 0
        override val width = width
        override val height = height
        override val pixels = IntArray(width * height)
        override val maxWidth = width
        override val maxHeight = height
        override var pixelIdx = ByteArray(0)
        override var palette = IntArray(0)
    }
}
