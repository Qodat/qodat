package stan.qodat.scene.runescape.ui

import qodat.cache.Cache
import qodat.cache.definition.InterfaceDefinition
import qodat.cache.definition.SpriteDefinition
import stan.qodat.scene.runescape.widget.WidgetLayout
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.math.min

/**
 * Software blit of one interface group, matching the client's 2D raster path:
 * hidden widgets drop their subtree, type-5 sprites draw at native size plus
 * [SpriteDefinition.offsetX]/[SpriteDefinition.offsetY], and layers clip children.
 *
 * This is the reference image for 2D mode and for comparing against OSRS.
 * It does not run CS2, so cache defaults (visibility, text, sprites) stay as packed.
 */
object InterfaceRaster {

    const val BACKGROUND = 0xFF2B2B2B.toInt()

    fun render(
        cache: Cache,
        definitions: List<InterfaceDefinition>,
        canvasWidth: Int = WidgetLayout.CANVAS_WIDTH,
        canvasHeight: Int = WidgetLayout.CANVAS_HEIGHT,
        background: Int = BACKGROUND,
        includeHidden: Boolean = false,
    ): BufferedImage = render(definitions, { id ->
        runCatching { cache.getSprite(id, 0) }.getOrNull()
    }, canvasWidth, canvasHeight, background, includeHidden)

    fun render(
        definitions: List<InterfaceDefinition>,
        sprite: (Int) -> SpriteDefinition?,
        canvasWidth: Int = WidgetLayout.CANVAS_WIDTH,
        canvasHeight: Int = WidgetLayout.CANVAS_HEIGHT,
        background: Int = BACKGROUND,
        includeHidden: Boolean = false,
    ): BufferedImage {
        val pixels = IntArray(canvasWidth * canvasHeight) { background }
        val clip = Clip(0, 0, canvasWidth, canvasHeight)
        for (root in WidgetLayout.buildHierarchy(definitions)) {
            paint(root, canvasWidth, canvasHeight, 0, 0, clip, pixels, canvasWidth, canvasHeight, sprite, includeHidden)
        }
        val image = BufferedImage(canvasWidth, canvasHeight, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, canvasWidth, canvasHeight, pixels, 0, canvasWidth)
        drawText(image, definitions, includeHidden)
        return image
    }

    private fun paint(
        node: WidgetLayout.HierarchyNode,
        parentWidth: Int,
        parentHeight: Int,
        originX: Int,
        originY: Int,
        clip: Clip,
        pixels: IntArray,
        canvasWidth: Int,
        canvasHeight: Int,
        sprite: (Int) -> SpriteDefinition?,
        includeHidden: Boolean,
    ) {
        val def = node.definition
        if (def.isHidden && !includeHidden)
            return
        val box = WidgetLayout.layout(def, parentWidth, parentHeight)
        val x = originX + box.x
        val y = originY + box.y
        when (def.type) {
            3, 10 -> fillRect(pixels, canvasWidth, canvasHeight, x, y, box.width, box.height, argb(def), def.filled, clip)
            5 -> blitGraphic(def, sprite, pixels, canvasWidth, canvasHeight, x, y, box, clip)
            9 -> fillRect(pixels, canvasWidth, canvasHeight, x, y, box.width, max(def.lineWidth, 1), argb(def), filled = true, clip)
        }
        val childClip = if (def.type == 0) clip.intersect(x, y, box.width, box.height) else clip
        if (childClip.isEmpty)
            return
        for (child in node.children) {
            paint(child, max(box.width, 0), max(box.height, 0), x, y, childClip, pixels, canvasWidth, canvasHeight, sprite, includeHidden)
        }
    }

    private fun blitGraphic(
        def: InterfaceDefinition,
        sprite: (Int) -> SpriteDefinition?,
        pixels: IntArray,
        canvasWidth: Int,
        canvasHeight: Int,
        x: Int,
        y: Int,
        box: WidgetLayout.Box,
        clip: Clip,
    ) {
        val source = def.spriteId.takeIf { it >= 0 }?.let(sprite) ?: return
        if (source.width <= 0 || source.height <= 0)
            return
        val opacity = 255 - (def.opacity and 0xff)
        if (def.spriteTiling) {
            var row = y
            while (row < y + box.height) {
                var col = x
                while (col < x + box.width) {
                    blitSprite(
                        pixels, canvasWidth, canvasHeight,
                        source, col, row, opacity,
                        def.flippedHorizontally, def.flippedVertically,
                        clip.intersect(x, y, box.width, box.height),
                    )
                    col += source.width
                }
                row += source.height
            }
            return
        }
        blitSprite(
            pixels, canvasWidth, canvasHeight,
            source, x + source.offsetX, y + source.offsetY, opacity,
            def.flippedHorizontally, def.flippedVertically, clip,
        )
    }

    private fun blitSprite(
        dest: IntArray,
        destWidth: Int,
        destHeight: Int,
        sprite: SpriteDefinition,
        destX: Int,
        destY: Int,
        opacity: Int,
        flipH: Boolean,
        flipV: Boolean,
        clip: Clip,
    ) {
        val src = sprite.pixels
        val sw = sprite.width
        val sh = sprite.height
        if (sw <= 0 || sh <= 0 || src.size < sw * sh)
            return
        val x0 = max(destX, clip.x)
        val y0 = max(destY, clip.y)
        val x1 = min(destX + sw, clip.x + clip.width)
        val y1 = min(destY + sh, clip.y + clip.height)
        if (x0 >= x1 || y0 >= y1)
            return
        val maxX = min(x1, destWidth)
        val maxY = min(y1, destHeight)
        val minX = max(x0, 0)
        val minY = max(y0, 0)
        for (py in minY until maxY) {
            val sy = if (flipV) sh - 1 - (py - destY) else py - destY
            for (px in minX until maxX) {
                val sx = if (flipH) sw - 1 - (px - destX) else px - destX
                val sample = src[sy * sw + sx]
                if (sample == 0)
                    continue
                val srcA = (sample ushr 24) and 0xff
                val alpha = if (srcA == 0) opacity else (srcA * opacity) / 255
                if (alpha <= 0)
                    continue
                val index = py * destWidth + px
                dest[index] = over(dest[index], sample or 0xFF000000.toInt(), alpha)
            }
        }
    }

    private fun fillRect(
        dest: IntArray,
        destWidth: Int,
        destHeight: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        color: Int,
        filled: Boolean,
        clip: Clip,
    ) {
        if (width <= 0 || height <= 0)
            return
        val alpha = (color ushr 24) and 0xff
        if (alpha <= 0)
            return
        if (!filled) {
            hLine(dest, destWidth, destHeight, x, y, width, color, clip)
            hLine(dest, destWidth, destHeight, x, y + height - 1, width, color, clip)
            vLine(dest, destWidth, destHeight, x, y, height, color, clip)
            vLine(dest, destWidth, destHeight, x + width - 1, y, height, color, clip)
            return
        }
        val x0 = max(x, max(clip.x, 0))
        val y0 = max(y, max(clip.y, 0))
        val x1 = min(x + width, min(clip.x + clip.width, destWidth))
        val y1 = min(y + height, min(clip.y + clip.height, destHeight))
        for (py in y0 until y1) {
            val row = py * destWidth
            for (px in x0 until x1)
                dest[row + px] = over(dest[row + px], color, alpha)
        }
    }

    private fun hLine(
        dest: IntArray, destWidth: Int, destHeight: Int,
        x: Int, y: Int, width: Int, color: Int, clip: Clip,
    ) {
        if (y !in 0 until destHeight || y < clip.y || y >= clip.y + clip.height)
            return
        val alpha = (color ushr 24) and 0xff
        val x0 = max(x, max(clip.x, 0))
        val x1 = min(x + width, min(clip.x + clip.width, destWidth))
        val row = y * destWidth
        for (px in x0 until x1)
            dest[row + px] = over(dest[row + px], color, alpha)
    }

    private fun vLine(
        dest: IntArray, destWidth: Int, destHeight: Int,
        x: Int, y: Int, height: Int, color: Int, clip: Clip,
    ) {
        if (x !in 0 until destWidth || x < clip.x || x >= clip.x + clip.width)
            return
        val alpha = (color ushr 24) and 0xff
        val y0 = max(y, max(clip.y, 0))
        val y1 = min(y + height, min(clip.y + clip.height, destHeight))
        for (py in y0 until y1)
            dest[py * destWidth + x] = over(dest[py * destWidth + x], color, alpha)
    }

    private fun drawText(image: BufferedImage, definitions: List<InterfaceDefinition>, includeHidden: Boolean) {
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        try {
            fun walk(node: WidgetLayout.HierarchyNode, parentWidth: Int, parentHeight: Int, originX: Int, originY: Int) {
                val def = node.definition
                if (def.isHidden && !includeHidden)
                    return
                val box = WidgetLayout.layout(def, parentWidth, parentHeight)
                val x = originX + box.x
                val y = originY + box.y
                if (def.type == 4) {
                    val text = stripTags(def.text.orEmpty())
                    if (text.isNotEmpty() && box.width > 0 && box.height > 0) {
                        val color = argb(def)
                        g.color = Color((color ushr 16) and 0xff, (color ushr 8) and 0xff, color and 0xff, (color ushr 24) and 0xff)
                        val size = when {
                            def.lineHeight > 0 -> def.lineHeight
                            box.height in 8..24 -> box.height
                            else -> 12
                        }
                        g.font = Font(Font.SANS_SERIF, Font.PLAIN, size.coerceIn(8, 18))
                        val fm = g.fontMetrics
                        val tx = when (def.xTextAlignment) {
                            1 -> x + (box.width - fm.stringWidth(text)) / 2
                            2 -> x + box.width - fm.stringWidth(text)
                            else -> x
                        }
                        val ty = when (def.yTextAlignment) {
                            1 -> y + (box.height + fm.ascent - fm.descent) / 2
                            2 -> y + box.height - fm.descent
                            else -> y + fm.ascent
                        }
                        val old = g.clip
                        g.clipRect(x, y, box.width, box.height)
                        if (def.textShadowed) {
                            g.color = Color(0, 0, 0, ((color ushr 24) and 0xff))
                            g.drawString(text, tx + 1, ty + 1)
                            g.color = Color((color ushr 16) and 0xff, (color ushr 8) and 0xff, color and 0xff, (color ushr 24) and 0xff)
                        }
                        g.drawString(text, tx, ty)
                        g.clip = old
                    }
                }
                for (child in node.children)
                    walk(child, max(box.width, 0), max(box.height, 0), x, y)
            }
            for (root in WidgetLayout.buildHierarchy(definitions))
                walk(root, WidgetLayout.CANVAS_WIDTH, WidgetLayout.CANVAS_HEIGHT, 0, 0)
        } finally {
            g.dispose()
        }
    }

    internal fun stripTags(text: String): String = TAG.replace(text, "")

    private fun argb(def: InterfaceDefinition): Int {
        val rgb = def.textColor
        val alpha = (255 - (def.opacity and 0xff)).coerceIn(0, 255)
        return (alpha shl 24) or (rgb and 0xFFFFFF)
    }

    private fun over(dst: Int, src: Int, alpha: Int): Int {
        if (alpha >= 255)
            return src or 0xFF000000.toInt()
        val inv = 255 - alpha
        val da = (dst ushr 24) and 0xff
        val outA = alpha + (da * inv) / 255
        fun mix(shift: Int): Int {
            val s = (src ushr shift) and 0xff
            val d = (dst ushr shift) and 0xff
            return (s * alpha + d * inv) / 255
        }
        return (outA shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }

    private data class Clip(val x: Int, val y: Int, val width: Int, val height: Int) {
        val isEmpty: Boolean get() = width <= 0 || height <= 0

        fun intersect(ix: Int, iy: Int, iw: Int, ih: Int): Clip {
            val x0 = max(x, ix)
            val y0 = max(y, iy)
            val x1 = min(x + width, ix + iw)
            val y1 = min(y + height, iy + ih)
            return Clip(x0, y0, max(0, x1 - x0), max(0, y1 - y0))
        }
    }

    private val TAG = Regex("<[^>]+>")
}
