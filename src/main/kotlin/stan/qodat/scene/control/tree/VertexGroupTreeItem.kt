package stan.qodat.scene.control.tree

import javafx.scene.Node
import javafx.scene.control.MultipleSelectionModel
import javafx.scene.control.SelectionMode
import javafx.scene.control.TreeItem
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.Box
import javafx.scene.shape.CullFace
import javafx.scene.shape.DrawMode
import javafx.scene.text.TextFlow
import stan.qodat.javafx.*
import stan.qodat.scene.runescape.model.Model
import stan.qodat.util.BABY_BLUE
import stan.qodat.util.DEFAULT
import stan.qodat.util.onInvalidation
import kotlin.math.abs

class VertexGroupTreeItem(private val model: Model,
                          index: Int,
                          private val vertexIndices: IntArray,
                          selectionModel: MultipleSelectionModel<TreeItem<Node>>
) : TreeItem<Node>() {

    private lateinit var selectionBox: Box

    init {
        text("VERTEX_GROUP", Color.web("#FFC66D"))
        label("$index (count = ${vertexIndices.size})")
        onExpanded {
            selectionModel.selectionMode = if (this) SelectionMode.MULTIPLE else SelectionMode.SINGLE
        }
        if (vertexIndices.isNotEmpty()) {
            for (vertex in vertexIndices) {
                val x = model.getX(vertex)
                val y = model.getY(vertex)
                val z = model.getZ(vertex)
                treeItem {
                    value = TextFlow().apply {
                        text("x = " to DEFAULT, "$x" to BABY_BLUE)
                        text(", y = " to DEFAULT, "$y" to BABY_BLUE)
                        text(", z = " to DEFAULT, "$z" to BABY_BLUE)
                    }
                }
            }
            selectionModel.onSelected { oldValue, newValue ->
                if (newValue == this) model.getSceneNode().children.add(getSelectionBox())
                else if (oldValue == this) model.getSceneNode().children.remove(getSelectionBox())
            }
        } else
            value.disableProperty().set(true)
    }

    private fun getSelectionBox() : Box {
        if (!this::selectionBox.isInitialized){
            selectionBox = Box()
            selectionBox.cullFace = CullFace.BACK
            selectionBox.drawMode = DrawMode.LINE
            selectionBox.material = PhongMaterial(Color.web("#CC7832"))
            model.getSceneNode().boundsInLocalProperty().onInvalidation {
                computeSelectionBoxBounds()
            }
            computeSelectionBoxBounds()
        }
        return selectionBox
    }

    private fun computeSelectionBoxBounds() {
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var minZ = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        var maxZ = Int.MIN_VALUE
        for (vertex in vertexIndices) {
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
        selectionBox.width = width.toDouble()
        selectionBox.height = height.toDouble()
        selectionBox.depth = depth.toDouble()
        selectionBox.translateX = (minX + width.toDouble().div(2))
        selectionBox.translateY = (minY + height.toDouble().div(2))
        selectionBox.translateZ = (minZ + depth.toDouble().div(2))
    }
}