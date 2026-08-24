package stan.qodat.scene.runescape.ui

import javafx.animation.Interpolator
import javafx.animation.TranslateTransition
import javafx.scene.Group
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.DrawMode
import javafx.scene.shape.MeshView
import javafx.util.Duration
import qodat.cache.definition.SpriteDefinition
import stan.qodat.scene.presentation.PlanarQuad
import stan.qodat.scene.presentation.PlanarView

/**
 * One sprite archive in the SubScene: the selected frame as a flat quad in 2D,
 * every frame exploded on Z in 3D (same toggle as interfaces).
 */
class SpriteSceneBuilder(
    private val selected: SpriteDefinition,
    archiveFrames: List<SpriteDefinition>,
) {

    private val frames = archiveFrames
        .filter { it.width > 0 && it.height > 0 }
        .ifEmpty { listOf(selected).filter { it.width > 0 && it.height > 0 } }
    private val frameNodes = ArrayList<Group>()

    fun build(): Group {
        val content = Group()
        for ((index, frame) in frames.withIndex()) {
            val image = Sprite.imageOf(frame)
            val holder = Group().apply {
                children.add(PlanarQuad.image(image))
                if (frame.frame == selected.frame)
                    children.add(highlightOf(image.width, image.height))
            }
            holder.properties["sprite-frame"] = index
            frameNodes.add(holder)
            content.children.add(holder)
        }
        centerOnOrigin(content)
        return content
    }

    fun applyExploded(exploded: Boolean, animate: Boolean) {
        for (node in frameNodes) {
            val index = (node.properties["sprite-frame"] as? Int) ?: 0
            val isSelected = frames.getOrNull(index)?.frame == selected.frame
            node.isVisible = exploded || isSelected
            val targetZ = if (exploded && index > 0) -index * PlanarView.LAYER_SPACING else 0.0
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

    private fun highlightOf(width: Double, height: Double): MeshView {
        return PlanarQuad.of(
            width,
            height,
            PhongMaterial(Color.web("#64dbfb")).apply { specularColor = Color.TRANSPARENT },
        ).apply {
            drawMode = DrawMode.LINE
            isMouseTransparent = true
        }
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
        fun archiveFrames(selected: SpriteDefinition, archive: List<SpriteDefinition>): List<SpriteDefinition> =
            archive.filter { it.id == selected.id && it.width > 0 && it.height > 0 }
                .sortedBy { it.frame }
                .ifEmpty { listOf(selected) }
    }
}
