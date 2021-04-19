package stan.qodat.scene.runescape.entity

import javafx.beans.property.SimpleStringProperty
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.TreeView
import javafx.scene.layout.HBox
import stan.qodat.cache.Cache
import stan.qodat.cache.definition.EntityDefinition
import stan.qodat.util.SceneNodeProvider
import stan.qodat.util.ViewNodeProvider
import stan.qodat.scene.control.tree.EntityTreeItem
import stan.qodat.scene.control.LabeledHBox
import stan.qodat.scene.runescape.model.Model
import stan.qodat.util.TreeItemProvider
import stan.qodat.util.Searchable

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   29/01/2021
 */
abstract class Entity<D : EntityDefinition>(
    private val cache: Cache,
    private val definition: D
) : Searchable, SceneNodeProvider, ViewNodeProvider, TreeItemProvider{

    private lateinit var modelGroup: Group
    private lateinit var models: Array<Model>
    private lateinit var viewBox : HBox
    private lateinit var treeItem: EntityTreeItem

    val labelProperty = SimpleStringProperty(definition.name)

    override fun getName() = labelProperty.get()

    fun getModels(): Array<Model> {
        if (!this::models.isInitialized){
            models = definition.modelIds.map {
                val modelDefinition = cache.getModel(it)
                Model("$it", modelDefinition)
            }.toTypedArray()

        }
        return models
    }

    override fun getViewNode(): Node {
        if (!this::viewBox.isInitialized)
            viewBox = LabeledHBox(labelProperty)
        return viewBox
    }

    override fun getSceneNode(): Group {
        if (!this::modelGroup.isInitialized){
            modelGroup = Group()
            for (model in getModels())
                modelGroup.children.add(model.getSceneNode())
        }
        return modelGroup
    }

    override fun getTreeItem(treeView: TreeView<Node>) : EntityTreeItem {
        if (!this::treeItem.isInitialized)
            treeItem = EntityTreeItem(this, treeView)
        return treeItem
    }
}