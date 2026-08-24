package stan.qodat.scene.runescape.ui

import stan.qodat.cache.impl.oldschool.definition.InterfaceDefinition
import stan.qodat.cache.impl.oldschool.definition.SpriteDefinition
import stan.qodat.scene.runescape.widget.WidgetLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class InterfaceRasterTest {

    @Test
    fun hiddenParentDropsVisibleChildren() {
        val defs = listOf(
            iface(0, parentId = -1) { isHidden = true; originalWidth = 40; originalHeight = 20 },
            iface(1, parentId = 0) {
                type = 3
                filled = true
                textColor = 0xFF0000
                originalWidth = 40
                originalHeight = 20
            },
        )
        val image = InterfaceRaster.render(defs, { null }, 40, 20, background = 0xFF000000.toInt())
        assertEquals(0xFF000000.toInt(), image.getRGB(5, 5))
    }

    @Test
    fun filledRectUsesRgbAndSkipsOutsideClip() {
        val defs = listOf(
            iface(0, parentId = -1) {
                type = 3
                filled = true
                textColor = 0x112233
                originalX = 2
                originalY = 3
                originalWidth = 4
                originalHeight = 2
            },
        )
        val image = InterfaceRaster.render(defs, { null }, 10, 10, background = 0xFF000000.toInt())
        assertEquals(0xFF112233.toInt(), image.getRGB(2, 3))
        assertEquals(0xFF000000.toInt(), image.getRGB(1, 3))
    }

    @Test
    fun spriteDrawsAtNativeSizePlusOffsetAndIsNotMirrored() {
        val red = 0xFFFF0000.toInt()
        val green = 0xFF00FF00.toInt()
        val sprite = sprite(2, 1, offsetX = 1, offsetY = 2, red, green)
        val defs = listOf(
            iface(0, parentId = -1) {
                type = 5
                spriteId = 7
                originalWidth = 20
                originalHeight = 20
            },
        )
        val image = InterfaceRaster.render(defs, { if (it == 7) sprite else null }, 20, 20, background = 0)
        assertEquals(red, image.getRGB(1, 2))
        assertEquals(green, image.getRGB(2, 2))
        assertEquals(0, image.getRGB(0, 2))
        assertEquals(0, image.getRGB(1, 1))
    }

    @Test
    fun layerClipsOverflowingChildSprite() {
        val white = 0xFFFFFFFF.toInt()
        val sprite = sprite(4, 1, offsetX = 0, offsetY = 0, white, white, white, white)
        val defs = listOf(
            iface(0, parentId = -1) {
                type = 0
                originalWidth = 2
                originalHeight = 2
            },
            iface(1, parentId = 0) {
                type = 5
                spriteId = 1
                originalWidth = 4
                originalHeight = 1
            },
        )
        val image = InterfaceRaster.render(defs, { sprite }, 8, 8, background = 0)
        assertEquals(white, image.getRGB(0, 0))
        assertEquals(white, image.getRGB(1, 0))
        assertEquals(0, image.getRGB(2, 0))
    }

    @Test
    fun stripTagsRemovesColourMarkup() {
        assertEquals("Bank", InterfaceRaster.stripTags("<col=ff9040>Bank</col>"))
    }

    @Test
    fun canvasMatchesClientFixedMode() {
        assertEquals(765, WidgetLayout.CANVAS_WIDTH)
        assertEquals(503, WidgetLayout.CANVAS_HEIGHT)
        val empty = InterfaceRaster.render(emptyList(), { null })
        assertEquals(765, empty.width)
        assertEquals(503, empty.height)
        assertTrue(empty.getRGB(0, 0) != 0)
        assertNotEquals(0, empty.getRGB(100, 100))
    }

    private fun iface(childId: Int, parentId: Int, init: InterfaceDefinition.() -> Unit) =
        InterfaceDefinition().also {
            it.id = childId
            it.parentId = parentId
            init(it)
        }

    private fun sprite(width: Int, height: Int, offsetX: Int, offsetY: Int, vararg pixels: Int) =
        SpriteDefinition().also {
            it.id = 1
            it.width = width
            it.height = height
            it.offsetX = offsetX
            it.offsetY = offsetY
            it.maxWidth = width + offsetX
            it.maxHeight = height + offsetY
            it.pixels = pixels
            it.pixelIdx = ByteArray(pixels.size)
            it.palette = IntArray(0)
        }
}
