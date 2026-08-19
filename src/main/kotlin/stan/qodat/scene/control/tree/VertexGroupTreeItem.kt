package stan.qodat.scene.control.tree

import javafx.scene.Node
import javafx.scene.control.MultipleSelectionModel
import javafx.scene.control.SelectionMode
import javafx.scene.control.TreeItem
import javafx.scene.text.TextFlow
import stan.qodat.javafx.*
import stan.qodat.scene.runescape.model.Model
import stan.qodat.scene.runescape.model.distinctColor
import stan.qodat.util.BABY_BLUE
import stan.qodat.util.DEFAULT

class VertexGroupTreeItem(
    private val model: Model,
    private val index: Int,
    private val vertexIndices: IntArray,
    selectionModel: MultipleSelectionModel<TreeItem<Node>>,
    nestedChildren: List<TreeItem<Node>> = emptyList(),
    highlightGroups: List<Int> = listOf(index)
) : TreeItem<Node>() {

    private val vertexPlaceholder = TreeItem<Node>().apply { label("…") }

    init {
        val skin = model.modelDefinition.getVertexSkins()?.let { skins ->
            vertexIndices.firstOrNull()?.let { skins[it] }
        } ?: index
        text("VERTEX_GROUP", distinctColor(skin))
        label("$index (skin = $skin, count = ${vertexIndices.size})")
        children.addAll(nestedChildren)
        if (vertexIndices.isNotEmpty())
            children.add(vertexPlaceholder)
        onExpanded {
            if (this) {
                selectionModel.selectionMode = SelectionMode.MULTIPLE
                if (children.remove(vertexPlaceholder))
                    addVertexChildren()
            }
        }
        if (vertexIndices.isNotEmpty()) {
            onTreeSelected(selectionModel) { oldValue, newValue ->
                if (newValue == this) model.addVertexGroupHighlights(highlightGroups)
                else if (oldValue == this) model.removeVertexGroupHighlights(highlightGroups)
            }
        } else
            value.disableProperty().set(true)
    }

    private fun addVertexChildren() {
        for (vertex in vertexIndices) {
            val x = model.getX(vertex)
            val y = model.getY(vertex)
            val z = model.getZ(vertex)
            treeItem {
                value = TextFlow().apply {
                    text("#$vertex  x = " to DEFAULT, "$x" to BABY_BLUE)
                    text(", y = " to DEFAULT, "$y" to BABY_BLUE)
                    text(", z = " to DEFAULT, "$z" to BABY_BLUE)
                }
            }
        }
    }
}
