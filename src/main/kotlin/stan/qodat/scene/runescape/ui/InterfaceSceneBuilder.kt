package stan.qodat.scene.runescape.ui

import javafx.animation.Interpolator
import javafx.animation.TranslateTransition
import javafx.beans.property.IntegerProperty
import javafx.embed.swing.SwingFXUtils
import javafx.geometry.VPos
import javafx.scene.DepthTest
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.DrawMode
import javafx.scene.shape.MeshView
import javafx.scene.text.Font
import javafx.scene.text.Text
import javafx.scene.text.TextAlignment
import javafx.scene.transform.Scale
import javafx.util.Duration
import qodat.cache.Cache
import qodat.cache.definition.InterfaceDefinition
import stan.qodat.scene.presentation.PlanarQuad
import stan.qodat.scene.presentation.PlanarView
import stan.qodat.scene.runescape.widget.WidgetLayout
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Builds a JavaFX 3D graph for one interface group using client layout rules,
 * then centers the used bounds on the origin.
 *
 * 2D mode shows a software blit ([InterfaceRaster]) so sprites are not mirrored
 * or Phong-lit. 3D mode explodes per-widget MeshView quads that face the camera.
 */
class InterfaceSceneBuilder(
    private val cache: Cache,
    private val definitions: List<InterfaceDefinition>,
    private val selectedChildId: IntegerProperty,
    private val hoveredChildId: IntegerProperty,
    private val onComponentClicked: (Int) -> Unit,
) {

    private val nodesByChildId = HashMap<Int, Group>()
    private val widgetVisuals = ArrayList<Node>()
    private var rasterNode: Node? = null

    fun build(): Group {
        val content = Group()
        content.children.add(rasterCanvas())
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
        rasterNode?.isVisible = !exploded
        for (visual in widgetVisuals)
            visual.isVisible = exploded
        for (node in nodesByChildId.values) {
            val depth = (node.properties["iface-depth"] as? Int) ?: 0
            val targetZ = if (exploded && depth > 0) -PlanarView.LAYER_SPACING else 0.0
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

    private fun rasterCanvas(): Node {
        val image = SwingFXUtils.toFXImage(InterfaceRaster.render(cache, definitions), null)
        return PlanarQuad.of(
            WidgetLayout.CANVAS_WIDTH.toDouble(),
            WidgetLayout.CANVAS_HEIGHT.toDouble(),
            PlanarQuad.unlit(image),
        ).also { rasterNode = it }
    }

    private fun buildNode(node: WidgetLayout.HierarchyNode, parentWidth: Int, parentHeight: Int, depth: Int): Group {
        val def = node.definition
        val childId = WidgetLayout.childId(def.id)
        val box = WidgetLayout.layout(def, parentWidth, parentHeight)
        if (def.isHidden)
            return Group()
        val group = Group().apply {
            id = "iface-$childId"
            properties["iface-depth"] = depth
            properties["iface-child"] = childId
            translateX = box.x.toDouble()
            translateY = -box.y.toDouble()
            depthTest = DepthTest.ENABLE
        }
        nodesByChildId[childId] = group

        visualFor(def, box)?.let {
            it.isVisible = false
            widgetVisuals.add(it)
            group.children.add(it)
        }
        if (box.width > 0 && box.height > 0)
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
        val width = box.width.toDouble()
        val height = box.height.toDouble()
        if (width <= 0.0 || height <= 0.0)
            return null
        return when (def.type) {
            3, 10 -> colorQuad(width, height, widgetColor(def.textColor, def.opacity), def.filled)
            4 -> textNode(def, width, height)
            5 -> graphicNode(def, width, height)
            9 -> colorQuad(width, max(def.lineWidth, 1).toDouble(), widgetColor(def.textColor, def.opacity), filled = true)
            else -> null
        }
    }

    private fun textNode(def: InterfaceDefinition, width: Double, height: Double): Node {
        val color = widgetColor(def.textColor, def.opacity)
        return Text(def.text.orEmpty()).apply {
            fill = color
            wrappingWidth = width
            font = Font.font(fontSizeFor(height, def.lineHeight))
            textOrigin = VPos.TOP
            textAlignment = when (def.xTextAlignment) {
                1 -> TextAlignment.CENTER
                2 -> TextAlignment.RIGHT
                else -> TextAlignment.LEFT
            }
            if (def.textShadowed)
                stroke = Color.rgb(0, 0, 0, color.opacity)
            // Client Y is down; 3D Y is up. Flip around the top-left so lines still read downward.
            transforms.add(Scale(1.0, -1.0, 1.0, 0.0, 0.0, 0.0))
        }
    }

    private fun graphicNode(def: InterfaceDefinition, width: Double, height: Double): Node? {
        val definition = def.spriteId.takeIf { it >= 0 }
            ?.let { runCatching { cache.getSprite(it, 0) }.getOrNull() }
            ?.takeIf { it.width > 0 && it.height > 0 }
            ?: return null
        val sprite = Sprite(definition)
        val image = sprite.image
        val material = PlanarQuad.unlit(image)
        val tileW = image.width
        val tileH = image.height
        val graphic = if (def.spriteTiling && tileW > 1 && tileH > 1) {
            val tiles = Group()
            val cols = ceil(width / tileW).toInt().coerceAtLeast(1)
            val rows = ceil(height / tileH).toInt().coerceAtLeast(1)
            for (col in 0 until cols) {
                for (row in 0 until rows) {
                    val w = min(tileW, width - col * tileW)
                    val h = min(tileH, height - row * tileH)
                    if (w <= 0 || h <= 0) continue
                    tiles.children.add(PlanarQuad.of(w, h, material).apply {
                        translateX = col * tileW
                        translateY = -row * tileH
                    })
                }
            }
            tiles
        } else {
            PlanarQuad.of(tileW, tileH, material).apply {
                translateX = definition.offsetX.toDouble()
                translateY = -definition.offsetY.toDouble()
            }
        }
        if (def.flippedHorizontally)
            graphic.scaleX = -1.0
        if (def.flippedVertically)
            graphic.scaleY = -1.0
        graphic.opacity = opacityOf(def.opacity)
        return graphic
    }

    private fun highlightOf(box: WidgetLayout.Box, childId: Int): MeshView {
        return PlanarQuad.of(
            box.width.toDouble(),
            box.height.toDouble(),
            PhongMaterial(Color.TRANSPARENT).apply { specularColor = Color.TRANSPARENT },
        ).apply {
            drawMode = DrawMode.LINE
            isMouseTransparent = true
            properties["iface-highlight"] = childId
        }
    }

    private fun childIdOn(node: Node?): Int? =
        generateSequence(node) { it.parent }.firstNotNullOfOrNull { it.properties["iface-child"] as? Int }

    private fun bindHighlights() {
        val refresh = {
            val selected = selectedChildId.get()
            val hovered = hoveredChildId.get()
            for (group in nodesByChildId.values) {
                val id = group.properties["iface-child"] as? Int ?: continue
                val highlight = group.children.firstOrNull { it.properties["iface-highlight"] != null } as? MeshView
                    ?: continue
                val material = highlight.material as? PhongMaterial ?: continue
                when (id) {
                    selected -> material.diffuseColor = Color.ALICEBLUE
                    hovered -> material.diffuseColor = Color.web("#64dbfb")
                    else -> material.diffuseColor = Color.TRANSPARENT
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
        private fun colorQuad(width: Double, height: Double, color: Color, filled: Boolean): MeshView {
            val quad = PlanarQuad.of(width, height, PhongMaterial(color).apply {
                specularColor = Color.BLACK
            })
            if (!filled)
                quad.drawMode = DrawMode.LINE
            return quad
        }

        private fun widgetColor(rgb: Int, opacity: Int): Color {
            val alpha = opacityOf(opacity)
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
