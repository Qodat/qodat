package stan.qodat.scene.runescape.entity

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.TreeView
import javafx.scene.layout.HBox
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import stan.qodat.cache.Cache
import stan.qodat.cache.definition.EntityDefinition
import stan.qodat.cache.impl.oldschool.RSModelDefinitionBuilder
import stan.qodat.javafx.text
import stan.qodat.scene.control.tree.EntityTreeItem
import stan.qodat.scene.control.LabeledHBox
import stan.qodat.scene.runescape.model.Model
import stan.qodat.util.*

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   29/01/2021
 */
abstract class Entity<D : EntityDefinition>(
    protected val cache: Cache,
    val definition: D
) : Searchable, SceneNodeProvider, ViewNodeProvider, TreeItemProvider{

    private lateinit var modelGroup: Group
    private lateinit var models: Array<Model>
    private lateinit var viewBox : HBox
    private lateinit var treeItem: EntityTreeItem

    val labelProperty = SimpleStringProperty(definition.name)
    val mergeModelProperty = SimpleBooleanProperty(true)

    override fun getName() = labelProperty.get()

    fun getModels(): Array<Model> {
        if (!this::models.isInitialized){
            try {
                val definitions = definition.modelIds.map { cache.getModelDefinition(it) }.toTypedArray()

                models = if (definitions.size > 1 && mergeModelProperty.get()) {
                    val multiModelName = "models_${definitions.joinToString {
                        it.getName() + "_"
                    }}"
                    arrayOf(Model(multiModelName, RSModelDefinitionBuilder(*definitions).build(), definition.findColor, definition.replaceColor))
                } else
                    createDistinctModels()
            } catch (e: Exception) {

            }
        }
        return models
    }

    fun getDistinctModels() = if(!mergeModelProperty.get() || definition.modelIds.size == 1)
        getModels()
    else
        createDistinctModels()

    private fun createDistinctModels() = definition.modelIds.map {
        val modelDefinition = cache.getModelDefinition(it)
        Model(it, modelDefinition, definition.findColor, definition.replaceColor)
    }.toTypedArray()

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