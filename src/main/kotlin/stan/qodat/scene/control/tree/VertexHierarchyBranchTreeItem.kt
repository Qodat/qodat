package stan.qodat.scene.control.tree

import javafx.scene.Node
import javafx.scene.control.MultipleSelectionModel
import javafx.scene.control.TreeItem
import javafx.scene.paint.Color
import stan.qodat.javafx.label
import stan.qodat.javafx.onTreeSelected
import stan.qodat.javafx.text
import stan.qodat.scene.runescape.model.Model
import stan.qodat.util.DEFAULT

internal class VertexHierarchyBranchTreeItem(
    model: Model,
    node: VertexHierarchyNode,
    selectionModel: MultipleSelectionModel<TreeItem<Node>>,
    childFactory: (VertexHierarchyNode) -> TreeItem<Node>
) : TreeItem<Node>() {

    init {
        when (node) {
            is TransformHierarchyNode -> {
                val vertexCount = node.highlightGroups.sumOf { model.getVertexGroups().getOrNull(it)?.size ?: 0 }
                text("TRANSFORM", Color.web("#FFC66D"))
                label("${node.index}  ${node.type}  (${node.targetGroups.size} groups, $vertexCount vertices)")
                children += node.children.map(childFactory)
            }
            is BucketHierarchyNode -> {
                text(node.title.uppercase().substringBefore(' '), DEFAULT)
                label("${node.title}  (${node.children.size})")
                children += node.children.map(childFactory)
            }
            is VertexGroupNode -> error("Vertex groups use VertexGroupTreeItem")
        }
        if (node.highlightGroups.isNotEmpty()) {
            val groups = node.highlightGroups
            onTreeSelected(selectionModel) { oldValue, newValue ->
                if (newValue == this) model.addVertexGroupHighlights(groups)
                else if (oldValue == this) model.removeVertexGroupHighlights(groups)
            }
        }
    }
}
