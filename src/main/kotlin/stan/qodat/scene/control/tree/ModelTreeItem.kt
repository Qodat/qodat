package stan.qodat.scene.control.tree

import javafx.collections.FXCollections
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.paint.Color
import javafx.scene.shape.DrawMode
import javafx.scene.text.Text
import stan.qodat.scene.runescape.model.Model
import stan.qodat.util.setAndBind

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

        selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->
            if (newValue == this)
                model.selectedProperty.set(true)
            else if (oldValue == this)
                model.selectedProperty.set(false)
        }

        val text = Text("MODEL").also {
            it.fill = Color.web("#FFC66D")
        }

        val label = Label().also {
            it.textProperty().setAndBind(model.labelProperty)
        }

        value = label
        graphic = text

        val visibleBox = CheckBox("visible").also {
            it.selectedProperty().setAndBind(model.visibleProperty, biDirectional = true)
        }
        val visibilityItem = TreeItem<Node>(visibleBox)
        children.add(visibilityItem)

        val drawModeValues = FXCollections.observableArrayList(*DrawMode.values())
        val drawModeBox = ComboBox(drawModeValues)
        drawModeBox.promptText = "Select draw mode"
        drawModeBox.selectionModel.select(model.drawModeProperty.get())
        drawModeBox.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            if (model.drawModeProperty.get() != newValue)
                model.drawModeProperty.set(newValue)
        }
        model.drawModeProperty.addListener { _, _, newValue ->
            if (drawModeBox.selectionModel.selectedItem != newValue)
                drawModeBox.selectionModel.select(newValue)
        }
        val drawModeItem = TreeItem<Node>(drawModeBox)
        children.add(drawModeItem)

        
        val vertexGroups = model.getVertexGroups()
        if (vertexGroups.isNotEmpty()){
            val vertexGroupsItem = TreeItem<Node>(Label("Vertex Groups"))
            children.add(vertexGroupsItem)
            for ((index, vertexGroup) in vertexGroups.withIndex()){
                val vertexGroupItem = VertexGroupTreeItem(model, index, vertexGroup, selectionModel)
                vertexGroupsItem.children.add(vertexGroupItem)
            }
        }

        val faceGroups = model.getFaceGroups()
        if (faceGroups.isNotEmpty()){
            val faceGroupsItem = TreeItem<Node>(Label("Face Groups"))
            children.add(faceGroupsItem)
            for ((index, faceGroup) in faceGroups.withIndex()){
                val faceGroupItem = FaceGroupTreeItem(model, index, faceGroup, selectionModel)
                faceGroupsItem.children.add(faceGroupItem)
            }
        }
    }
}