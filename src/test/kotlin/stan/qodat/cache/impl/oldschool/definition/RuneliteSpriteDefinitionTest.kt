package stan.qodat.cache.impl.oldschool.definition

import net.runelite.cache.definitions.SpriteDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RuneliteSpriteDefinitionTest {

    @Test
    fun copiesSpriteIdentityAndPixelBuffers() {
        val src = SpriteDefinition().apply {
            id = 17
            frame = 2
            offsetX = 3
            offsetY = 4
            width = 2
            height = 1
            maxWidth = 8
            maxHeight = 8
            pixels = intArrayOf(0xFF00FF00.toInt(), 0xFF0000FF.toInt())
            pixelIdx = byteArrayOf(1, 2)
            palette = intArrayOf(0, 0x00FF00, 0x0000FF)
        }

        val mapped = RuneliteSpriteDefinition(src)
        assertEquals(17, mapped.id)
        assertEquals(2, mapped.frame)
        assertEquals(3, mapped.offsetX)
        assertEquals(4, mapped.offsetY)
        assertEquals(2, mapped.width)
        assertEquals(1, mapped.height)
        assertEquals(8, mapped.maxWidth)
        assertEquals(8, mapped.maxHeight)
        assertTrue(mapped.pixels.contentEquals(intArrayOf(0xFF00FF00.toInt(), 0xFF0000FF.toInt())))
        assertTrue(mapped.pixelIdx.contentEquals(byteArrayOf(1, 2)))
        assertTrue(mapped.palette.contentEquals(intArrayOf(0, 0x00FF00, 0x0000FF)))
        assertTrue(mapped.toString().contains("id=17"))
    }
}
