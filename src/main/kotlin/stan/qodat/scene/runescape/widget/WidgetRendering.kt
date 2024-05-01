package stan.qodat.scene.runescape.widget

import javafx.scene.Group
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import qodat.cache.Cache
import stan.qodat.scene.runescape.ui.Sprite
import stan.qodat.scene.runescape.widget.component.Component
import stan.qodat.scene.runescape.widget.component.Pos
import stan.qodat.scene.runescape.widget.component.Size
import stan.qodat.scene.runescape.widget.component.Sprites
import stan.qodat.scene.runescape.widget.component.impl.*
import stan.qodat.util.ModelUtil
import tornadofx.imageview
import tornadofx.line
import tornadofx.rectangle
import kotlin.math.max
import kotlin.math.min

data class DrawDimensions(
    val width: Int,
    val height: Int,
    val x: Int,
    val y: Int,
)

class WidgetRenderer(
    val cache: Cache,
) {

    class RenderContext(
        val nestedLevel: Int,
        val maxWidth: Int,
        val maxHeight: Int,
        val clipLeft: Int,
        val clipTop: Int,
        val clipRight: Int,
        val clipBottom: Int,
        val absXOffset: Int,
        val absYOffset: Int,
    ) {
        private val drawDimensionsMap = mutableMapOf<Component<*>, DrawDimensions>()
        private val Component<*>.drawDimensions
            get() = drawDimensionsMap.getOrPut(this) {
                DrawDimensions(
                    width = when (hSize) {
                        Size.abs -> width
                        Size.minus -> maxWidth - width
                        Size.proportion -> maxWidth * width shr 14
                        else -> error("Unhandled hSize $hSize")
                    },
                    height = when (vSize) {
                        Size.abs -> height
                        Size.minus -> maxHeight - height
                        Size.proportion -> maxHeight * height shr 14
                        else -> error("Unhandled vSize $vSize")
                    },
                    x = when (hPos) {
                        Pos.abs_left -> x
                        Pos.abs_centre -> x + (maxWidth - width) / 2
                        Pos.abs_right -> maxWidth - width - x
                        Pos.proportion_left -> (maxWidth * x) shr 14
                        Pos.proportion_centre -> ((maxWidth * x) shr 14) + ((maxWidth - width) / 2)
                        Pos.proportion_right -> (maxWidth - width - ((x * maxWidth) shr 14))
                        else -> error("Unhandled hPos $hPos")
                    },
                    y = when (vPos) {
                        Pos.abs_top -> y
                        Pos.abs_centre -> ((maxHeight - height) / 2) + y
                        Pos.abs_bottom -> maxHeight - height - y
                        Pos.proportion_top -> (maxHeight * y) shr 14
                        Pos.proportion_centre -> ((maxHeight * y) shr 14) + ((maxHeight - height) / 2)
                        Pos.proportion_bottom -> (maxHeight - height - ((maxHeight * y) shr 14))
                        else -> error("Unhandled hPos $hPos")
                    }
                )
            }
        val Component<*>.drawWidth get() = drawDimensions.width
        val Component<*>.drawHeight get() = drawDimensions.height
        val Component<*>.drawX get() = drawDimensions.x
        val Component<*>.drawY get() = drawDimensions.y

        fun debugLn(message: String) {
            repeat(nestedLevel) { print('\t') }
            println(message)
        }
    }

    fun render(widget: Widget, width: Int, height: Int): Group {
        val context = RenderContext(0, width, height, 0, 0, width, height, 0, 0)
        return context.renderComponents(widget.children)
    }

    private fun RenderContext.renderComponents(components: List<Component<*>>) = Group().apply {
        for (component in components) {
//            setClip(clipLeft, clipTop, clipRight, clipBottom)
            if (component.hidden) {
                debugLn("Skipping rendering of $component because it is hidden")
                continue
            }
            val absX = component.drawX + absXOffset
            val absY = component.drawY + absYOffset
            val (startX, startY, endX, endY) = if (component is Inventory) {
                listOf(clipLeft, clipTop, clipRight, clipBottom)
            } else {
                var absWidth: Int = component.drawWidth + absX
                var absHeight: Int = component.drawHeight + absY
                if (component is Line) {
                    absWidth++
                    absHeight++
                }
                listOf(max(clipLeft, absX), max(clipTop, absY), min(clipRight, absWidth), min(clipBottom, absHeight))
            }
            if (startX < endX && startY < endY) {

                debugLn("Rendering at ($absX, $absY) in bounds ($startX..$endX, $startY..$endY) - $component")
                children.add(when (component) {
                    is Layer -> renderLayer(component, absX, absY, startX, startY, endX, endY)
                    is Rectangle -> renderRectangle(component, absX, absY)
                    is Text -> renderText(component, absX, absY)
                    is Graphic -> renderGraphic(component, absX, absY)
                    else -> continue
                })
            } else {
                debugLn("Not rendering at ($absX, $absY) in bounds ($startX..$endX, $startY..$endY) - $component")
            }
        }
    }


    val field643: Int = 16777215.inv() or 2301979
    val field584: Int = 16777215.inv() or 5063219
    val field646: Int = 16777215.inv() or 7759444
    val field836: Int = 16777215.inv() or 3353893
    private fun RenderContext.renderLayer(
        component: Layer,
        absX: Int,
        absY: Int,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
    ): Pane = with(component) {
        val context = RenderContext(
            nestedLevel + 1,
            endX - startX,
            endY - startY,
            startX,
            startY,
            endX,
            endY,
            absX - component.scrollX,
            absY - component.scrollY
        )
        val pane = Pane()
        val subChildren: Group = context.renderComponents(children)
        pane.children.addAll(subChildren)

        val scrollHeight = scrollHeight
        if (scrollHeight > drawHeight) {
            val scrollBarX = absX + drawWidth
            pane.imageview(Sprite(cache.getSprite(316, 0)).image) {
                x = scrollBarX.toDouble()
                y = absY.toDouble()
            }
            pane.imageview(Sprite(cache.getSprite(316, 1)).image) {
                x = scrollBarX.toDouble()
                y = absY + drawHeight - 16.toDouble()
            }
            pane.rectangle(
                x = scrollBarX,
                y = absY + 16,
                width = 16,
                height = drawHeight - 32,
            ) {
                fill = ModelUtil.hsbToColor(field643, 100)
            }

            val scrollBarHeight = ((drawHeight * (drawHeight - 32)) / scrollHeight).coerceAtLeast(8)
            val int_23: Int = scrollY * (drawHeight - 32 - scrollBarHeight) / (scrollHeight - drawHeight)
            pane.rectangle(
                x = scrollBarX,
                y = absY + int_23 + 16,
                width = 16,
                height = scrollBarHeight,
            ) {
                fill = ModelUtil.hsbToColor(field584, 100)
            }
            pane.line(
                startX = scrollBarX.toDouble(),
                startY = absY + int_23 + 16.toDouble(),
                endX = scrollBarX.toDouble(),
                endY = int_23 + absY + 16 + scrollBarHeight.toDouble(),
            ) {
                stroke = ModelUtil.hsbToColor(field646, 100)
            }
            pane.line(
                startX = scrollBarX + 1.toDouble(),
                startY = absY + int_23 + 16.toDouble(),
                endX = scrollBarX + 1.toDouble(),
                endY = int_23 + absY + 16 + scrollBarHeight.toDouble(),
            ) {
                stroke = ModelUtil.hsbToColor(field646, 100)
            }
            pane.line(
                startX = scrollBarX.toDouble(),
                startY = absY + int_23 + 16.toDouble(),
                endX = scrollBarX + 16.toDouble(),
                endY = int_23 + absY + 16.toDouble(),
            ) {
                stroke = ModelUtil.hsbToColor(field646, 100)
            }
            pane.line(
                startX = scrollBarX.toDouble(),
                startY = int_23 + absY + 17.toDouble(),
                endX = scrollBarX + 16.toDouble(),
                endY = int_23 + absY + 17.toDouble(),
            ) {
                stroke = ModelUtil.hsbToColor(field646, 100)
            }
            pane.line(
                startX = scrollBarX + 15.toDouble(),
                startY = absY + int_23 + 16.toDouble(),
                endX = scrollBarX + 15.toDouble(),
                endY = int_23 + absY + 16 + scrollBarHeight.toDouble(),
            ) {
                stroke = ModelUtil.hsbToColor(field836, 100)
            }
            pane.line(
                startX = scrollBarX + 14.toDouble(),
                startY = int_23 + absY + 17.toDouble(),
                endX = scrollBarX + 14.toDouble(),
                endY = int_23 + absY + 16 + scrollBarHeight - 1.toDouble(),
            ) {
                stroke = ModelUtil.hsbToColor(field836, 100)
            }
            pane.line(
                startX = scrollBarX.toDouble(),
                startY = absY + int_23 + scrollHeight + 15.toDouble(),
                endX = scrollBarX + 16.toDouble(),
                endY = int_23 + absY + scrollHeight + 15.toDouble(),
            ) {
                stroke = ModelUtil.hsbToColor(field836, 100)
            }
            pane.line(
                startX = scrollBarX + 1.toDouble(),
                startY = int_23 + absY + scrollHeight + 14.toDouble(),
                endX = scrollBarX + 15.toDouble(),
                endY = int_23 + absY + scrollHeight + 14.toDouble(),
            ) {
                stroke = ModelUtil.hsbToColor(field836, 100)
            }

//            toolkit.fillRectangle(scrollBarX, absY + int_23 + 16, 16, scrollBarHeight, field584, 1)
//            toolkit.drawVerticalLine(scrollBarX, int_23 + absY + 16, scrollBarHeight, field646, 1)
//            toolkit.drawVerticalLine(scrollBarX + 1, absY + int_23 + 16, scrollBarHeight, field646, 1)
//            toolkit.drawHorizontalLine(scrollBarX, absY + int_23 + 16, 16, field646, 1)
//            toolkit.drawHorizontalLine(scrollBarX, int_23 + absY + 17, 16, field646, 1)
//            toolkit.drawVerticalLine(scrollBarX + 15, absY + int_23 + 16, scrollBarHeight, field836, 1)
//            toolkit.drawVerticalLine(scrollBarX + 14, int_23 + absY + 17, scrollBarHeight - 1, field836, 1)
//            toolkit.drawHorizontalLine(scrollBarX, absY + int_23 + scrollHeight + 15, 16, field836, 1)
//            toolkit.drawHorizontalLine(scrollBarX + 1, int_23 + absY + scrollHeight + 14, 15, field836, 1)
        }
        pane
    }

    private fun RenderContext.renderRectangle(component: Rectangle, x: Int, y: Int): javafx.scene.shape.Rectangle = with(component) {
        val color = if (trans == 0)
            0xffffff.inv() or (colour?.value ?: 0)
        else
            255 - (trans and 0xff) shl 24 or ((colour?.value ?: 0) and 0xffffff)
        javafx.scene.shape.Rectangle(x.toDouble(), y.toDouble()).apply {
            width = drawWidth.toDouble()
            height = drawHeight.toDouble()
            if (filled)
                fill = ModelUtil.hsbToColor(color, 100)
            else
                stroke = ModelUtil.hsbToColor(color, 100)
        }
    }

    private fun RenderContext.renderText(
        component: Text,
        absX: Int,
        absY: Int,
    ) = with(component) {
        Group()
//        var shadowColor: Int = (255 - (trans and 0xff))
//        if (shadowColor == 0)
//            return@with
//        val font = font?.run(this@WidgetRenderer::createFont)
//            ?: error("Did not find font $font for text $component")
//        val text = itemId
//            ?.run {
//                val itemDef = cache.getItemDefinition(this)
//                val text = itemDef?.name ?: "null"
//                itemAmount
//                    ?.takeIf { itemDef.stackable == 1 || it > 1 }
//                    ?.run { getColTags(16748608) + text + aString94 + " x" + method7251(this) }
//                    ?: text
//            }
//            ?: text
//        if (clipWidgetComponents)
//            toolkit.ensureClips(absX, absY, drawWidth + absX, drawHeight + absY)
//        shadowColor = shadowColor shl 24
//        font.drawString(
//            text,
//            absX,
//            absY,
//            drawWidth,
//            drawHeight,
//            shadowColor or (colour?.value ?: 0),
//            if (shadowed) shadowColor else -1,
//            hAlign.id,
//            vAlign.id,
//            lineHeight,
//            0,
//            cache.crownSprites,
//            null,
//            null,
//            0,
//            0,
//            cache
//        )
//        if (clipWidgetComponents)
//            toolkit.setClip(clipLeft, clipTop, clipRight, clipBottom)
    }

//    private fun createFont(font: Font): JagexFont {
//        val group = cache.getSpriteGroupDefinition(font.id)
//        if (group?.list.isNullOrEmpty())
//            error("Sprite group is null or empty for font $font (group = $group)")
//        val fontMetrics = try {
//            cache.getFontMetrics(font.id)
//                ?: error("Did not find font metrics for font $font")
//        } catch (e: Exception) {
//            throw Exception("Failed to load font metrics for font $font", e)
//        }
//        val sprites = group.toArray().filterIsInstance<IndexedSprite2>().toTypedArray()
//        return toolkit.createFont(fontMetrics, sprites, false)
//    }

    private fun RenderContext.renderGraphic(
        component: Graphic,
        absX: Int,
        absY: Int,
    ): ImageView = with(component) {
        val sprite: ImageView =
            sprite?.run { createSprite(this, flipH, flipV, borderThickness, shadowColour?.value ?: 0) }
                ?:return@with ImageView()

        val spriteWidth: Int = sprite.fitWidth.toInt()
        val spriteHeight: Int = sprite.fitHeight.toInt()
        val spriteColour: Int = 255 - (trans and 0xff)
        if (spriteColour == 0) {
            debugLn("Not rendering $component because spriteColour == 0 ")
            return@with ImageView()
        }
        val colour = (colour?.value
            ?.takeUnless { it == -1 }
            ?.run { this and 0xffffff }
            ?.coerceAtLeast(16777215)
            ?: 16777215) or (spriteColour shl 24)
        val blend = -1 != colour
        val is317 = false
        if (repeatSprite) {
//            toolkit.ensureClips(absX, absY, drawWidth + absX, drawHeight + absY)
            if (spriteRotation != 0) {
                val chunkX: Int = (drawWidth + (spriteWidth - 1)) / spriteWidth
                val chunkY: Int = (spriteHeight - 1 + drawHeight) / spriteHeight
                for (dx in 0 until chunkX) {
                    for (dy in 0 until chunkY) {
                        val drawX = spriteWidth.toFloat() / 2.0f + (dx * spriteWidth + absX).toFloat()
                        val drawY = (dy * spriteHeight + absY).toFloat() + spriteHeight.toFloat() / 2.0f
//                        if (blend)
//                            sprite.drawRotated(drawX, drawY, 4096, spriteRotation, 0, colour, 1)
//                        else
//                            sprite.drawRotated(drawX, drawY, 4096, spriteRotation)
                    }
                }
            }
//            else if (blend) {
//
//                sprite.drawRepeated(absX, absY, drawWidth, drawHeight, 0, colour, 1)
//            }else
//                sprite.drawRepeated(absX, absY, drawWidth, drawHeight)
//            toolkit.setClip(clipLeft, clipTop, clipRight, clipBottom);
        } else if (blend) {
//            if (spriteRotation != 0)
//                sprite.drawRotated(
//                    drawWidth.toFloat() / 2.0f + absX.toFloat(),
//                    absY.toFloat() + drawHeight.toFloat() / 2.0f,
//                    4096 * drawWidth / spriteWidth,
//                    spriteRotation,
//                    0,
//                    colour,
//                    1
//                )
//            else if (drawWidth != spriteWidth || spriteHeight != drawHeight)
//                sprite.drawScaled(absX, absY, drawWidth, drawHeight, 0, colour, 1)
//            else
//                sprite.draw(absX, absY, 0, colour, 1)
        }
//        else if (spriteRotation != 0)
//            sprite.drawRotated(
//                drawWidth.toFloat() / 2.0f + absX.toFloat(),
//                drawHeight.toFloat() / 2.0f + absY.toFloat(),
//                4096 * drawWidth / spriteWidth,
//                spriteRotation
//            )
//        else if (!is317 && (drawWidth != spriteWidth || drawHeight != spriteHeight))
//            sprite.drawScaled(absX, absY, drawWidth, drawHeight)
//        else
//            sprite.draw(absX, absY)
        sprite?:ImageView()
    }

    private fun createSprite(
        sprites: Sprites,
        flipH: Boolean,
        flipV: Boolean,
        borderThickness: Int,
        shadowColour: Int,
    ): ImageView {
        val sprite = ImageView(Sprite(cache.getSprite(sprites.groupId, sprites.index)).image)
        if (flipV) sprite.scaleY = -1.0
        if (flipH) sprite.scaleX = -1.0
//        if (borderThickness > 0) indexedSprite.outlineToSprite(borderThickness)
//        else if (shadowColour != 0) indexedSprite.outlineToSprite(1)
//        if (borderThickness >= 1) indexedSprite.addOutline(1)
//        if (borderThickness >= 2) indexedSprite.addOutline(16777215)
//        if (shadowColour != 0) indexedSprite.fillTransparentAreas(0xffffff.inv() or shadowColour)
//        return toolkit.createSprite(indexedSprite, false)
        return sprite
    }

    private fun RenderContext.renderModel(
        component: Model,
        absX: Int,
        absY: Int,
    ) = with(component) {
//        toolkit.method7493()
//
//        val currentBrightness = 0
//
//        toolkit.setAmbientIntensity((0.7f + currentBrightness.toFloat() * 0.1f) * 1.1523438f)
//        toolkit.setSun(anInt4824, 0.69921875f, 1.2f, -200.0f, -240.0f, -200.0f)
//        toolkit.setFog(DEFAULT_SKY_COLOR, -1, 0)
//
//        // TOOD: finish
        Group()
    }
}
