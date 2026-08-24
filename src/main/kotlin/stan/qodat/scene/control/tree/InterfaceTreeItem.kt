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
            val component = generateSequence(item) { it.parent }
                .firstNotNullOfOrNull { it as? InterfaceComponentTreeItem }
            val childId = component?.let { WidgetLayout.childId(it.definition.id) } ?: -1
            if (group.selectedChildId.get() != childId)
                group.selectedChildId.set(childId)
        }
        group.selectedChildId.addListener { _, _, childId ->
            val item = itemsByChildId[childId.toInt()] ?: return@addListener
            if (selectionModel.selectedItem !== item)
                selectionModel.select(item)
        }

        expandedProperty().set(group.treeItemExpandedProperty().get())
    }
}
