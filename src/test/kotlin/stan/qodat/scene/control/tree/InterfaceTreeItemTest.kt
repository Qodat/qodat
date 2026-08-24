package stan.qodat.scene.control.tree

import javafx.scene.control.TreeItem
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InterfaceTreeItemTest {

    @Test
    fun belongsToAndAlreadyShowsWalkAncestors() {
        val root = TreeItem<Any>()
        val child = TreeItem<Any>()
        val other = TreeItem<Any>()
        root.children.add(child)
        assertTrue(InterfaceTreeItem.belongsTo(child, root))
        assertFalse(InterfaceTreeItem.belongsTo(other, root))
        assertTrue(InterfaceTreeItem.alreadyShows(child, root))
        assertTrue(InterfaceTreeItem.alreadyShows(root, root))
        assertFalse(InterfaceTreeItem.alreadyShows(other, root))
    }
}
