package stan.qodat.scene.control.tree

import javafx.scene.Node
import javafx.scene.control.MultipleSelectionModel
import javafx.scene.control.TreeItem
import javafx.scene.paint.Color
import stan.qodat.javafx.label
import stan.qodat.javafx.text
import stan.qodat.javafx.treeItem
import stan.qodat.scene.runescape.ui.InterfaceGroup
import stan.qodat.scene.runescape.widget.WidgetLayout

class InterfaceTreeItem(val group: InterfaceGroup, val selectionModel: MultipleSelectionModel<TreeItem<Node>>) :
    TreeItem<Node>() {

    private val itemsByChildId = HashMap<Int, InterfaceComponentTreeItem>()
    private var syncing = false

    init {
        label(group.nameProperty)
        text(group.javaClass.simpleName, Color.web("#FFC66D"))

        treeItem("Components") {
            val pending = group.definitions.map { definition ->
                InterfaceComponentTreeItem(group.cache, definition).also { item ->
                    itemsByChildId[WidgetLayout.childId(definition.id)] = item
                }
            }.toMutableList()
            var progress = true
            while (pending.isNotEmpty() && progress) {
                progress = false
                val iterator = pending.listIterator()
                while (iterator.hasNext()) {
                    val next = iterator.next()
                    val parentKey = WidgetLayout.parentChildId(next.definition.parentId)
                    if (parentKey == -1) {
                        children.add(next)
                        iterator.remove()
                        progress = true
                    } else {
                        val parent = itemsByChildId[parentKey]
                        if (parent != null && parent !== next) {
                            parent.children.add(next)
                            iterator.remove()
                            progress = true
                        }
                    }
                }
            }
            pending.forEach { children.add(it) }
        }

        selectionModel.selectedItemProperty().addListener { _, _, item ->
            if (syncing || !belongsTo(item, this))
                return@addListener
            val childId = childIdOf(item)
            if (group.selectedChildId.get() != childId)
                runSync { group.selectedChildId.set(childId) }
        }
        group.selectedChildId.addListener { _, _, childId ->
            val item = itemsByChildId[childId.toInt()] ?: return@addListener
            if (syncing || alreadyShows(selectionModel.selectedItem, item))
                return@addListener
            runSync {
                expandAncestors(item)
                selectionModel.select(item)
            }
        }

        expandedProperty().set(group.treeItemExpandedProperty().get())
    }

    private fun runSync(block: () -> Unit) {
        syncing = true
        try {
            block()
        } finally {
            syncing = false
        }
    }

    companion object {
        internal fun childIdOf(item: TreeItem<*>?): Int {
            val component = generateSequence(item) { it.parent }
                .firstNotNullOfOrNull { it as? InterfaceComponentTreeItem }
                ?: return -1
            return WidgetLayout.childId(component.definition.id)
        }

        internal fun belongsTo(item: TreeItem<*>?, root: TreeItem<*>): Boolean =
            generateSequence(item) { it.parent }.any { it === root }

        internal fun alreadyShows(selected: TreeItem<*>?, target: TreeItem<*>): Boolean =
            generateSequence(selected) { it.parent }.any { it === target }

        internal fun expandAncestors(item: TreeItem<*>) {
            generateSequence(item.parent) { it.parent }.forEach { it.isExpanded = true }
        }
    }
}
