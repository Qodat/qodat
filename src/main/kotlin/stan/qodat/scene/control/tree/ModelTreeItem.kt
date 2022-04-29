package stan.qodat.scene.control.tree

import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.MultipleSelectionModel
import javafx.scene.control.TreeItem
import javafx.scene.paint.Color
import javafx.scene.shape.DrawMode
import stan.qodat.Properties
import stan.qodat.javafx.*
import stan.qodat.scene.control.export.ExportMenu
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
        label(model.labelProperty) {
            contextMenu = ContextMenu(
                ExportMenu<Model>().apply {
                    setExportable(model)
                }
            )
        }
        treeItem("Render Options") {
            treeItem {
                vBox {
                    checkBox("shading", model.shadingProperty, biDirectional = true)
                    checkBox("show priorities", model.displayFacePriorityLabelsProperty, biDirectional = true)
                    checkBox("visible", model.visibleProperty, biDirectional = true)
                    comboBox("Select draw mode", DrawMode.values(), model.drawModeProperty, biDirectional = true)
                }
            }
        }
        treeItem("Details") {
            treeItem { label("vertexCount = ${model.modelDefinition.getVertexCount()}") }
            treeItem { label("faceCount = ${model.modelDefinition.getFaceCount()}") }
            val vGroups = treeItem("Vertex Groups")
            val fGroups = treeItem("Face Groups")
            onExpanded {
                if (this) {
                    if (vGroups.children.isEmpty()) {
                        val vertexGroups = model.getVertexGroups()
                        for ((index, vertexGroup) in vertexGroups.withIndex())
                            vGroups.children += VertexGroupTreeItem(model, index, vertexGroup, selectionModel)
                    }
                    if (fGroups.children.isEmpty()) {
                        val faceGroups = model.getFaceGroups()
                        for ((index, faceGroup) in faceGroups.withIndex())
                            fGroups.children += FaceGroupTreeItem(model, index, faceGroup, selectionModel)
                    }
                }
            }
        }
        selectionModel.onSelected { oldValue, newValue ->
            if (newValue == this) model.selectedProperty.set(true)
            else if (oldValue == this) model.selectedProperty.set(false)
        }
        expandedProperty().set(Properties.treeItemModelsExpanded.get())
    }
}