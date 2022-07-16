package stan.qodat.scene.provider

import javafx.beans.property.BooleanProperty
import javafx.scene.Node
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   31/01/2021
 */
interface TreeItemProvider {

    fun treeItemExpandedProperty() : BooleanProperty

    fun getTreeItem(treeView: TreeView<Node>) : TreeItem<Node>

    fun removeTreeItemReference() {}
}
