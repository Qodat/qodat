package stan.qodat.scene.runescape.ui

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.value.ChangeListener
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.TreeView
import javafx.scene.layout.HBox
import qodat.cache.Cache
import qodat.cache.definition.InterfaceDefinition
import stan.qodat.Properties
import stan.qodat.scene.control.LabeledHBox
import stan.qodat.scene.control.tree.InterfaceTreeItem
import stan.qodat.scene.provider.SceneNodeProvider
import stan.qodat.scene.provider.TreeItemProvider
import stan.qodat.scene.provider.ViewNodeProvider
import stan.qodat.util.Searchable

class InterfaceGroup(val cache: Cache, private val groupId: Int, val definitions: List<InterfaceDefinition>) :
    SceneNodeProvider, ViewNodeProvider, TreeItemProvider, Searchable {

    val idProperty = SimpleIntegerProperty(groupId)
    val nameProperty = SimpleStringProperty(idProperty.get().toString())
    val selectedChildId = SimpleIntegerProperty(-1)
    val hoveredChildId = SimpleIntegerProperty(-1)

    private val viewBox: HBox by lazy {
        LabeledHBox(nameProperty, labelPrefix = "widget")
    }

    private var sceneBuilder: InterfaceSceneBuilder? = null
    private val explodedListener = ChangeListener<Boolean> { _, _, exploded ->
        sceneBuilder?.applyExploded(exploded, animate = true)
    }

    private val sceneGroup: Group by lazy {
        val builder = InterfaceSceneBuilder(
            cache = cache,
            definitions = definitions,
            selectedChildId = selectedChildId,
            hoveredChildId = hoveredChildId,
            onComponentClicked = { selectedChildId.set(it) },
        )
        sceneBuilder = builder
        val content = builder.build()
        builder.applyExploded(InterfacePresentation.exploded.get(), animate = false)
        InterfacePresentation.exploded.addListener(explodedListener)
        Group(content)
    }

    private lateinit var treeItem: InterfaceTreeItem

    override fun getSceneNode(): Group {
        val node = sceneGroup
        InterfacePresentation.exploded.removeListener(explodedListener)
        InterfacePresentation.exploded.addListener(explodedListener)
        sceneBuilder?.applyExploded(InterfacePresentation.exploded.get(), animate = false)
        return node
    }

    override fun getViewNode() = viewBox

    override fun getTreeItem(treeView: TreeView<Node>): InterfaceTreeItem {
        if (!this::treeItem.isInitialized)
            treeItem = InterfaceTreeItem(this, treeView.selectionModel)
        return treeItem
    }

    override fun removeSceneNodeReference() {
        InterfacePresentation.exploded.removeListener(explodedListener)
    }

    override fun getName() = nameProperty.get()

    override fun treeItemExpandedProperty(): BooleanProperty =
        Properties.treeItemInterfaceExpanded
}
