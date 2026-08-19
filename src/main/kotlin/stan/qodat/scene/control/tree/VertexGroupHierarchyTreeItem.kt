package stan.qodat.scene.control.tree

import javafx.beans.InvalidationListener
import javafx.beans.WeakInvalidationListener
import javafx.scene.Node
import javafx.scene.control.MultipleSelectionModel
import javafx.scene.control.SelectionMode
import javafx.scene.control.TreeItem
import javafx.scene.paint.Color
import stan.qodat.Properties
import stan.qodat.javafx.label
import stan.qodat.javafx.onExpanded
import stan.qodat.javafx.text
import stan.qodat.scene.runescape.animation.AnimationLegacy
import stan.qodat.scene.runescape.animation.AnimationSkeleton
import stan.qodat.scene.runescape.entity.AnimatedEntity
import stan.qodat.scene.runescape.model.Model

class VertexGroupHierarchyTreeItem(
    private val model: Model,
    private val selectionModel: MultipleSelectionModel<TreeItem<Node>>
) : TreeItem<Node>() {

    private val animationListener = InvalidationListener {
        model.clearVertexGroupHighlights()
        if (isExpanded)
            rebuild()
    }

    init {
        text("HIERARCHY", Color.web("#FFC66D"))
        label("Vertex Group Hierarchy")
        onExpanded {
            selectionModel.selectionMode = if (this) SelectionMode.MULTIPLE else SelectionMode.SINGLE
            if (this && children.isEmpty())
                rebuild()
        }
        Properties.selectedAnimation.addListener(WeakInvalidationListener(animationListener))
    }

    private fun rebuild() {
        children.clear()
        val (title, roots) = VertexGroupHierarchy.build(model, resolveSkeletons())
        label(title)
        if (roots.isEmpty()) {
            children += TreeItem<Node>().apply { label("No non-empty vertex groups") }
            return
        }
        for (root in roots)
            children += toTreeItem(root)
    }

    private fun toTreeItem(node: VertexHierarchyNode): TreeItem<Node> = when (node) {
        is VertexGroupNode -> VertexGroupTreeItem(
            model,
            node.index,
            node.vertices,
            selectionModel,
            nestedChildren = node.children.map { toTreeItem(it) },
            highlightGroups = node.highlightGroups
        )
        is TransformHierarchyNode -> VertexHierarchyBranchTreeItem(model, node, selectionModel) { toTreeItem(it) }
        is BucketHierarchyNode -> VertexHierarchyBranchTreeItem(model, node, selectionModel) { toTreeItem(it) }
    }

    private fun resolveSkeletons(): List<Pair<Int, AnimationSkeleton>> {
        val animation = Properties.selectedAnimation.get()
            ?: (Properties.selectedEntity.get() as? AnimatedEntity<*>)?.selectedAnimation?.get()
            ?: return emptyList()
        if (animation !is AnimationLegacy)
            return emptyList()
        return animation.getSkeletons().entries
            .sortedBy { it.key }
            .map { it.key to it.value }
    }
}
