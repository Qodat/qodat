package stan.qodat.javafx

import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.scene.control.Menu
import javafx.scene.control.TreeItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JavaFXDSLTest {

    @Test
    fun treeItemAppendsAChildAndRunsInit() {
        val root = TreeItem("root")
        val child = root.treeItem {
            value = "child"
        }
        assertEquals(1, root.children.size)
        assertEquals(child, root.children[0])
        assertEquals("child", child.value)
        assertEquals(root, child.parent)

        child.treeItem { value = "grandchild" }
        assertEquals(1, child.children.size)
        assertEquals("grandchild", child.children[0].value)
    }

    @Test
    fun onExpandedReceivesTheNewExpandedFlag() {
        val item = TreeItem("x")
        val seen = mutableListOf<Boolean>()
        item.onExpanded { seen += this }
        item.isExpanded = true
        item.isExpanded = false
        assertEquals(listOf(true, false), seen)
    }

    @Test
    fun onChangeReceivesListMutations() {
        val list = FXCollections.observableArrayList<String>()
        val added = mutableListOf<String>()
        val removedItems = mutableListOf<String>()
        list.onChange {
            next()
            if (wasAdded()) added += addedSubList
            if (wasRemoved()) removedItems += removed
        }
        list.add("a")
        list.addAll("b", "c")
        list.remove("b")
        assertEquals(listOf("a", "b", "c"), added)
        assertEquals(listOf("b"), removedItems)
    }

    @Test
    fun menuItemAppendsANamedAction() {
        val menu = Menu("File")
        var ran = false
        menu.menuItem("Open") { ran = true }
        assertEquals(1, menu.items.size)
        assertEquals("Open", menu.items[0].text)
        menu.items[0].onAction.handle(ActionEvent())
        assertTrue(ran)
    }
}
