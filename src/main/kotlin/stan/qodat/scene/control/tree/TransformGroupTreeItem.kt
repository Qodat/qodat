package stan.qodat.scene.control.tree

import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.Box
import javafx.scene.shape.CullFace
import javafx.scene.shape.DrawMode
import javafx.scene.shape.Sphere
import stan.qodat.javafx.label
import stan.qodat.javafx.onExpanded
import stan.qodat.javafx.onSelected
import stan.qodat.javafx.text
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.animation.Transformation
import stan.qodat.scene.runescape.entity.AnimatedEntity
import stan.qodat.scene.runescape.model.Model
import stan.qodat.util.BABY_BLUE
import stan.qodat.util.onInvalidation
import kotlin.math.abs

class TransformGroupTreeItem(
    private val entity: AnimatedEntity<*>,
    frame: AnimationFrame,
    frameItem: TreeItem<Node>,
    treeView: TreeView<Node>,
    rootTransformation: Transformation,
    private val childTransformations: List<Transformation>
) : TreeItem<Node>() {

    init {
        text("TRANSFORMATIONS", Color.web("#FFC66D"))
        label("${childTransformations.size}")
        val root = TransformTreeItem(entity, frame, rootTransformation, frameItem, treeView.selectionModel)
        children.add(root)
        onExpanded {
            root.children.clear()
            for ((index, transform) in childTransformations.withIndex())
                root.children.add(
                    index,
                    TransformTreeItem(entity, frame, transform, root, treeView.selectionModel))
        }
        treeView.selectionModel.onSelected { oldValue, newValue ->
            if (newValue == this) {
                if (oldValue !is TransformTreeItem)
                    for (model in entity.getModels())
                        model.drawModeProperty.set(DrawMode.LINE)
                entity.getSceneNode().children.addAll(getSelectionMesh())
            } else if (oldValue == this) {
                if (newValue !is TransformTreeItem)
                    for (model in entity.getModels())
                        model.drawModeProperty.set(DrawMode.FILL)
                entity.getSceneNode().children.removeAll(getSelectionMesh())
            }
        }
    }

    private lateinit var selectionMesh: Group

    private fun getSelectionMesh(): Group {
        if (!this::selectionMesh.isInitialized) {
            val selectionMaterial = PhongMaterial(BABY_BLUE)
            selectionMesh = Group()
            var meshId = 0
            for (transform in childTransformations) {
                for (i in 0 until transform.groupIndices.size()) {
                    val groupIndex = transform.groupIndices[i]
                    for (model in entity.getModels()) {
                        val vertices = model.getVertexGroups().getOrNull(groupIndex) ?: continue
                        selectionMesh.children.add(getSelectionBox(meshId++, model, vertices))
                    }
                }
            }
        }
        return selectionMesh
    }
    private fun getSelectionBox(meshId: Int, model: Model, vertices: IntArray) : Sphere {
        val selectionBox = Sphere()
        selectionBox.id = "$meshId"
        selectionBox.cullFace = CullFace.FRONT
        selectionBox.drawMode = DrawMode.LINE
        selectionBox.material = PhongMaterial(Color.web("#CC7832"))
        model.getSceneNode().boundsInLocalProperty().onInvalidation {
            computerCenter(selectionBox, model, vertices)
        }
        computerCenter(selectionBox, model, vertices)

        return selectionBox
    }

    private fun computerCenter(sphere: Sphere, model: Model, vertices: IntArray) {
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var minZ = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        var maxZ = Int.MIN_VALUE
        for (vertex in vertices) {
            val x = model.getX(vertex)
            val y = model.getY(vertex)
            val z = model.getZ(vertex)
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
            if (z < minZ) minZ = z
            if (z > maxZ) maxZ = z
        }
        val width = abs(maxX - minX)
        val height = abs(maxY - minY)
        val depth = abs(maxZ - minZ)
        val box = Box()
//        box.width = width.toDouble()
//        box.height = height.toDouble()
//        box.depth = depth.toDouble()
        sphere.radius = 3.0
        sphere.translateX = (minX + width.toDouble().div(2))
        sphere.translateY = (minY + height.toDouble().div(2))
        sphere.translateZ = (minZ + depth.toDouble().div(2))
    }
}