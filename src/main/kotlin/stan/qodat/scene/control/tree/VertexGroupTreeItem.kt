package stan.qodat.scene.control.tree

import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.MultipleSelectionModel
import javafx.scene.control.TreeItem
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.Box
import javafx.scene.shape.CullFace
import javafx.scene.shape.DrawMode
import javafx.scene.text.FontSmoothingType
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import stan.qodat.scene.runescape.model.Model
import stan.qodat.util.BABY_BLUE
import stan.qodat.util.onInvalidation
import kotlin.math.abs

class VertexGroupTreeItem(private val model: Model,
                          index: Int,
                          private val vertexIndices: IntArray,
                          selectionModel: MultipleSelectionModel<TreeItem<Node>>
) : TreeItem<Node>() {

    private lateinit var selectionBox: Box

    init {
        value = Label("$index (count = ${vertexIndices.size})")
        graphic = Text("VERTEX_GROUP").also {
            it.fill = Color.web("#FFC66D")
        }
        if (vertexIndices.isNotEmpty()) {
            for (vertex in vertexIndices) {
                val x = model.getX(vertex)
                val y = model.getY(vertex)
                val z = model.getZ(vertex)
                val textFlow = TextFlow()
                textFlow.addText("x = ")
                textFlow.addText("$x", BABY_BLUE)
                textFlow.addText(", y = ")
                textFlow.addText("$y", BABY_BLUE)
                textFlow.addText(", z = ")
                textFlow.addText("$z", BABY_BLUE)
                val item = TreeItem<Node>(textFlow)
                children.add(item)
            }
            selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->
                if (newValue == this) {
                    model.getSceneNode().children.add(getSelectionBox())
                } else if (oldValue == this) {
                    model.getSceneNode().children.remove(getSelectionBox())
                }
            }
        } else
            value.disableProperty().set(true)
    }

    private fun TextFlow.addText(string: String, color: Color = Color.web("#A9B7C6")) {
        children.add(Text(string).also {
            it.fontSmoothingType = FontSmoothingType.GRAY
            it.fill = color
        })
    }

    private fun getSelectionBox() : Box {
        if (!this::selectionBox.isInitialized){
            selectionBox = Box()
//                selectionBox.depthTest = DepthTest.DISABLE
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