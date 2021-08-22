package stan.qodat.scene.control.tree

import javafx.scene.Node
import javafx.scene.control.MultipleSelectionModel
import javafx.scene.control.TreeItem
import javafx.scene.paint.Color
import javafx.scene.shape.DrawMode
import stan.qodat.javafx.*
import stan.qodat.scene.runescape.model.Model

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   01/02/2021
 */
class ModelTreeItem(
        model: Model,
        selectionModel: MultipleSelectionModel<TreeItem<Node>>
) : TreeItem<Node>() {

    init {
        text("MODEL", Color.web("#FFC66D"))
        label(model.labelProperty)
        treeItem { checkBox("shading", model.shadingProperty, biDirectional = true) }
        treeItem { checkBox("show priorities", model.displayFacePriorityLabelsProperty, biDirectional = true) }
        treeItem { checkBox("visible", model.visibleProperty, biDirectional = true) }
        treeItem { comboBox("Select draw mode", DrawMode.values(), model.drawModeProperty, biDirectional = true) }
        treeItem { label("vertexCount = ${model.modelDefinition.getVertexCount()}") }
        treeItem { label("faceCount = ${model.modelDefinition.getFaceCount()}") }
        val vertexGroups = model.getVertexGroups()
        if (vertexGroups.isNotEmpty()){
            treeItem("Vertex Groups") {
                for ((index, vertexGroup) in vertexGroups.withIndex())
                    children += VertexGroupTreeItem(model, index, vertexGroup, selectionModel)
            }
        }
        val faceGroups = model.getFaceGroups()
        if (faceGroups.isNotEmpty()){
            treeItem("Face Groups") {
                for ((index, faceGroup) in faceGroups.withIndex())
                    children += FaceGroupTreeItem(model, index, faceGroup, selectionModel)
            }
        }
        selectionModel.onSelected { oldValue, newValue ->
            if (newValue == this) model.selectedProperty.set(true)
            else if (oldValue == this) model.selectedProperty.set(false)
        }
    }
}