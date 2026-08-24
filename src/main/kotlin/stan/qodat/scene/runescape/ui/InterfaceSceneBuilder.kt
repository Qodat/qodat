package stan.qodat.scene.runescape.ui

import javafx.animation.Interpolator
import javafx.animation.TranslateTransition
import javafx.beans.property.IntegerProperty
import javafx.scene.DepthTest
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.image.ImageView
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.paint.ImagePattern
import javafx.scene.shape.Line
import javafx.scene.shape.Rectangle
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import javafx.util.Duration
import qodat.cache.Cache
import qodat.cache.definition.InterfaceDefinition
import stan.qodat.scene.runescape.widget.WidgetLayout
import kotlin.math.max

/**
 * Builds a JavaFX graph for one interface group using client layout rules,
 * then centers the used bounds on the origin.
 */
class InterfaceSceneBuilder(
    private val cache: Cache,
    private val definitions: List<InterfaceDefinition>,
    private val selectedChildId: IntegerProperty,
    private val hoveredChildId: IntegerProperty,
    private val onComponentClicked: (Int) -> Unit,
) {

    private val nodesByChildId = HashMap<Int, Group>()

    fun build(): Group {
        val content = Group()
        val roots = WidgetLayout.buildHierarchy(definitions)
        for (root in roots) {
            content.children.add(buildNode(root, WidgetLayout.CANVAS_WIDTH, WidgetLayout.CANVAS_HEIGHT, 0))
        }
        centerOnOrigin(content)
        bindHighlights()
        content.addEventFilter(MouseEvent.MOUSE_MOVED) { event ->
            hoveredChildId.set(childIdOn(event.target as? Node) ?: -1)
        }
        content.addEventHandler(MouseEvent.MOUSE_EXITED) {
            hoveredChildId.set(-1)
        }
        return content
    }

    fun applyExploded(exploded: Boolean, animate: Boolean) {
        for (node in nodesByChildId.values) {
            val depth = (node.properties["iface-depth"] as? Int) ?: 0
            val targetZ = if (exploded && depth > 0) -LAYER_SPACING else 0.0
            if (!animate) {
                node.translateZ = targetZ
                continue
            }
            TranslateTransition(Duration.millis(if (exploded) 450.0 else 250.0), node).apply {
                toZ = targetZ
                interpolator = Interpolator.EASE_BOTH
                play()
            }
        }
    }

    private fun buildNode(node: WidgetLayout.HierarchyNode, parentWidth: Int, parentHeight: Int, depth: Int): Group {
        val def = node.definition
        val childId = WidgetLayout.childId(def.id)
        val box = WidgetLayout.layout(def, parentWidth, parentHeight)
        if (def.isHidden) {
            val holder = Group().apply {
                translateX = box.x.toDouble()
                translateY = -box.y.toDouble()
                isMouseTransparent = true
            }
            for (child in node.children) {
                holder.children.add(buildNode(child, max(box.width, 0), max(box.height, 0), depth))
            }
            return holder
        }
        val group = Group().apply {
            id = "iface-$childId"
            properties["iface-depth"] = depth
            properties["iface-child"] = childId
            translateX = box.x.toDouble()
            translateY = -box.y.toDouble()
            depthTest = DepthTest.ENABLE
        }
        nodesByChildId[childId] = group

        val visual = visualFor(def, box) ?: hitArea(box)
        visual.scaleY = -1.0
        visual.depthTest = DepthTest.ENABLE
        visual.isPickOnBounds = true
        group.children.add(visual)
        group.children.add(highlightOf(box, childId))
        group.setOnMouseClicked {
            onComponentClicked(childId)
            it.consume()
        }

        for (child in node.children) {
            group.children.add(buildNode(child, max(box.width, 0), max(box.height, 0), depth + 1))
        }
        return group
    }

    private fun visualFor(def: InterfaceDefinition, box: WidgetLayout.Box): Node? {
        val width = max(box.width, 0).toDouble()
        val height = max(box.height, 0).toDouble()
        return when (def.type) {
            3, 10 -> Rectangle(width, height).apply {
                val color = widgetColor(def.textColor, def.opacity)
                if (def.filled) fill = color else {
                    fill = Color.TRANSPARENT
                    stroke = color
                    strokeWidth = max(def.lineWidth, 1).toDouble()
                }
            }
            4 -> Text(def.text.orEmpty()).apply {
                val color = widgetColor(def.textColor, def.opacity)
                fill = color
                wrappingWidth = width
                font = Font.font(fontSizeFor(height, def.lineHeight))
                textAlignment = when (def.xTextAlignment) {
                    1 -> TextAlignment.CENTER
                    2 -> TextAlignment.RIGHT
                    else -> TextAlignment.LEFT
                }
                if (def.textShadowed)
                    stroke = Color.rgb(0, 0, 0, color.opacity)
            }
            5 -> graphicNode(def, width, height)
            9 -> Line().apply {
                if (def.lineDirection) {
                    startX = 0.0
                    startY = height
                    endX = width
                    endY = 0.0
                } else {
                    startX = 0.0
                    startY = 0.0
                    endX = width
                    endY = height
                }
                stroke = widgetColor(def.textColor, def.opacity)
                strokeWidth = max(def.lineWidth, 1).toDouble()
            }
            2 -> Rectangle(width, height).apply {
                fill = Color.rgb(40, 40, 48, 0.25)
                stroke = Color.web("#8aa2b8")
                strokeDashArray.addAll(4.0, 3.0)
            }
            6 -> Rectangle(width, height).apply {
                fill = Color.rgb(70, 58, 40, 0.35)
                stroke = Color.web("#c9a227")
                strokeDashArray.addAll(3.0, 3.0)
            }
            else -> null
        }
    }

    private fun graphicNode(def: InterfaceDefinition, width: Double, height: Double): Node {
        val sprite = def.spriteId.takeIf { it >= 0 }
            ?.let { runCatching { cache.getSprite(it, 0) }.getOrNull() }
            ?.let { Sprite(it) }
            ?: return hitArea(WidgetLayout.Box(0, 0, width.toInt(), height.toInt()))
        val image = sprite.image
        val graphic: Node = if (def.spriteTiling && image.width > 0 && image.height > 0) {
            Rectangle(width, height).apply {
                fill = ImagePattern(image, 0.0, 0.0, image.width, image.height, false)
                opacity = opacityOf(def.opacity)
            }
        } else {
            ImageView(image).apply {
                fitWidth = width.coerceAtLeast(1.0)
                fitHeight = height.coerceAtLeast(1.0)
                isPreserveRatio = false
                opacity = opacityOf(def.opacity)
            }
        }
        if (def.flippedHorizontally) {
            graphic.scaleX = -1.0
            graphic.translateX = width
        }
        if (def.flippedVertically) {
            graphic.scaleY = -1.0
            graphic.translateY = height
        }
        return Group(graphic)
    }

    private fun hitArea(box: WidgetLayout.Box) = Rectangle(
        max(box.width, 1).toDouble(),
        max(box.height, 1).toDouble(),
        Color.TRANSPARENT,
    )

    private fun highlightOf(box: WidgetLayout.Box, childId: Int) = Rectangle(
        max(box.width, 1).toDouble(),
        max(box.height, 1).toDouble(),
    ).apply {
        fill = Color.TRANSPARENT
        stroke = Color.TRANSPARENT
        strokeWidth = 1.5
        isMouseTransparent = true
        scaleY = -1.0
        properties["iface-highlight"] = childId
    }

    private fun childIdOn(node: Node?): Int? =
        generateSequence(node) { it.parent }.firstNotNullOfOrNull { it.properties["iface-child"] as? Int }

    private fun bindHighlights() {
        val refresh = {
            val selected = selectedChildId.get()
            val hovered = hoveredChildId.get()
            for (group in nodesByChildId.values) {
                val id = group.properties["iface-child"] as? Int ?: continue
                val highlight = group.children.firstOrNull { it.properties["iface-highlight"] != null } as? Rectangle
                    ?: continue
                when (id) {
                    selected -> {
                        highlight.stroke = Color.ALICEBLUE
                        highlight.strokeWidth = 2.0
                        highlight.strokeDashArray.clear()
                    }
                    hovered -> {
                        highlight.stroke = Color.web("#64dbfb")
                        highlight.strokeWidth = 1.5
                        highlight.strokeDashArray.setAll(5.0, 3.0)
                    }
                    else -> {
                        highlight.stroke = Color.TRANSPARENT
                        highlight.strokeDashArray.clear()
                    }
                }
            }
        }
        selectedChildId.addListener { _, _, _ -> refresh() }
        hoveredChildId.addListener { _, _, _ -> refresh() }
    }

    private fun centerOnOrigin(content: Group) {
        content.layout()
        val bounds = content.boundsInLocal
        if (!bounds.width.isFinite() || !bounds.height.isFinite())
            return
        content.translateX = -bounds.centerX
        content.translateY = -bounds.centerY
    }

    companion object {
        const val LAYER_SPACING = 56.0

        private fun widgetColor(rgb: Int, opacity: Int): Color {
            val alpha = ((255 - (opacity and 0xff)) / 255.0).coerceIn(0.0, 1.0)
            return Color.rgb((rgb ushr 16) and 0xff, (rgb ushr 8) and 0xff, rgb and 0xff, alpha)
        }

        private fun opacityOf(opacity: Int) =
            ((255 - (opacity and 0xff)) / 255.0).coerceIn(0.0, 1.0)

        private fun fontSizeFor(height: Double, lineHeight: Int): Double {
            val fromLine = if (lineHeight > 0) lineHeight.toDouble() else 0.0
            return max(fromLine, if (height > 0) (height * 0.7).coerceIn(9.0, 16.0) else 12.0)
        }
    }
}
