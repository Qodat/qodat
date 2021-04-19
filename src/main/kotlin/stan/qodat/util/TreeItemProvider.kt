package stan.qodat.util

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

    fun getTreeItem(treeView: TreeView<Node>) : TreeItem<Node>
}