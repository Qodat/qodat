package stan.qodat.scene.control.tree

import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.MultipleSelectionModel
import javafx.scene.control.TreeItem
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.DrawMode
import javafx.scene.text.FontSmoothingType
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import stan.qodat.scene.runescape.model.Model
import stan.qodat.scene.runescape.model.ModelFaceMesh
import stan.qodat.util.BABY_BLUE

class FaceGroupTreeItem(private val model: Model,
                        index: Int,
                        private val faceIndices: IntArray,
                        selectionModel: MultipleSelectionModel<TreeItem<Node>>
) : TreeItem<Node>() {

    private lateinit var selectionMesh: Group

    init {
        value = Label("$index (count = ${faceIndices.size})")
        graphic = Text("FACE_GROUP").also {
            it.fill = Color.web("#FFC66D")
        }
        if (faceIndices.isNotEmpty()) {
            for (face in faceIndices) {
                val (v1, v2, v3) = model.getVertices(face)
                // TODO: add x,y,z coordinates for each vertex
                val textFlow = TextFlow()
                textFlow.addText("v1 = ")
                textFlow.addText("$v1", BABY_BLUE)
                textFlow.addText(", v2 = ")
                textFlow.addText("$v2", BABY_BLUE)
                textFlow.addText(", v3 = ")
                textFlow.addText("$v3", BABY_BLUE)
                val item = TreeItem<Node>(textFlow)
                children.add(item)
            }
            selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->
                if (newValue == this) {
                    if (oldValue !is FaceGroupTreeItem)
                        model.drawModeProperty.set(DrawMode.LINE)
                    model.getSceneNode().children.add(getSelectionMesh())
                } else if (oldValue == this) {
                    if (newValue !is FaceGroupTreeItem)
                        model.drawModeProperty.set(DrawMode.FILL)
                    model.getSceneNode().children.remove(getSelectionMesh())
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
    private fun getSelectionMesh(): Group {
        if (!this::selectionMesh.isInitialized) {
            val selectionMaterial = PhongMaterial(BABY_BLUE)
            selectionMesh = Group()
            for (face in faceIndices) {
                val mesh = ModelFaceMesh(face, selectionMaterial)
                val (v1, v2, v3) = model.getVertices(face)
                val vertexIndex1 = mesh.addVertex(v1, model.getX(v1), model.getY(v1), model.getZ(v1))
                val vertexIndex2 = mesh.addVertex(v2, model.getX(v2), model.getY(v2), model.getZ(v2))
                val vertexIndex3 = mesh.addVertex(v3, model.getX(v3), model.getY(v3), model.getZ(v3))
                val u = floatArrayOf(-1f, -1f, -1f)
                val v = floatArrayOf(-1f, -1f, -1f)
                val texIndex1 = mesh.addUV(u[0], v[0])
                val texIndex2 = mesh.addUV(u[1], v[1])
                val texIndex3 = mesh.addUV(u[2], v[2])
                mesh.faces.addAll(
                    vertexIndex1, texIndex1,
                    vertexIndex2, texIndex2,
                    vertexIndex3, texIndex3
                )
                mesh.drawModeProperty.set(DrawMode.FILL)
                mesh.visibleProperty.set(true)
                selectionMesh.children.add(mesh.getSceneNode())
            }
        }
        return selectionMesh
    }
}