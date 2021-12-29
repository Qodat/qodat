package stan.qodat.scene.runescape.ui

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.layout.HBox
import javafx.scene.shape.Rectangle
import stan.qodat.cache.Cache
import stan.qodat.cache.definition.InterfaceDefinition
import stan.qodat.scene.control.LabeledHBox
import stan.qodat.scene.control.tree.InterfaceTreeItem
import stan.qodat.util.SceneNodeProvider
import stan.qodat.util.Searchable
import stan.qodat.util.TreeItemProvider
import stan.qodat.util.ViewNodeProvider

class InterfaceGroup(val cache: Cache, private val groupId: Int, val definitions: List<InterfaceDefinition>) :
    SceneNodeProvider, ViewNodeProvider, TreeItemProvider, Searchable {

    val idProperty = SimpleIntegerProperty(groupId)
    val nameProperty = SimpleStringProperty(idProperty.get().toString())

    private val viewBox: HBox by lazy {
        LabeledHBox(nameProperty)
    }

    private val sceneGroup: Group by lazy {
        val group = Group()
        group
    }

    private lateinit var treeItem: InterfaceTreeItem

    override fun getSceneNode() = sceneGroup

    override fun getViewNode() = viewBox

    override fun getTreeItem(treeView: TreeView<Node>): InterfaceTreeItem {
        if (!this::treeItem.isInitialized)
            treeItem = InterfaceTreeItem(this, treeView.selectionModel)
        return treeItem
    }

    override fun getName() = nameProperty.get()

}