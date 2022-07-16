package stan.qodat.scene.runescape.entity

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.TreeView
import javafx.scene.layout.HBox
import javafx.scene.text.TextFlow
import qodat.cache.Cache
import qodat.cache.definition.EntityDefinition
import qodat.cache.models.RS2ModelBuilder
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.javafx.menloText
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.control.LabeledHBox
import stan.qodat.scene.control.ViewNodeListView
import stan.qodat.scene.control.export.Exportable
import stan.qodat.scene.control.tree.EntityTreeItem
import stan.qodat.scene.controller.EntityViewController
import stan.qodat.scene.paint.Material
import stan.qodat.scene.provider.SceneNodeProvider
import stan.qodat.scene.provider.TreeItemProvider
import stan.qodat.scene.provider.ViewNodeProvider
import stan.qodat.scene.runescape.model.Model
import stan.qodat.util.DEFAULT
import stan.qodat.util.formatName
import stan.qodat.util.getMaterial

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   29/01/2021
 */
abstract class Entity<D : EntityDefinition>(
    protected val cache: Cache,
    val definition: D
) : Exportable, SceneNodeProvider, ViewNodeProvider, TreeItemProvider {

    private var modelGroup: Group? = null
    private var models: Array<Model>? = null
    private lateinit var materials: Array<Material>
    private lateinit var viewBox: HBox
    private var treeItem: EntityTreeItem? = null

    val locked = SimpleBooleanProperty(false).apply {
        addListener { _, oldValue, newValue ->
            if (oldValue == true && newValue == false) {
                (SubScene3D.contextProperty.get()?.getController() as? EntityViewController)
                    ?.onUnselectedEvent
                    ?.handle(
                        ViewNodeListView.UnselectedEvent(
                            viewNodeProvider = this@Entity,
                            hasNewValueOfSameType = false,
                            causedByTabSwitch = false
                        )
                    )
            }
        }
    }
    val labelProperty = SimpleStringProperty(definition.name)
    val mergeModelProperty = SimpleBooleanProperty(true)

    abstract fun property(): SimpleStringProperty

    override fun getName() = labelProperty.get()

    fun getModels(): Array<Model> {
        if (models == null) {
            try {
                val definitions = definition.modelIds.map { cache.getModelDefinition(it) }.toTypedArray()
                models = if (definitions.size > 1 && mergeModelProperty.get()) {
                    val multiModelName = "models_${
                        definitions.joinToString {
                            it.getName() + "_"
                        }
                    }"
                    val modelDefinition = RS2ModelBuilder(*definitions).build()
                    val model = Model(multiModelName, modelDefinition, definition.findColor, definition.replaceColor)
                    arrayOf(model)
                } else
                    createDistinctModels()
            } catch (e: Throwable) {
                Qodat.logException("Could not get entity {${getName()}}'s models", e)
                return emptyArray()
            }
        }
        return models!!
    }

    fun getMaterials(): Array<Material> {
        if (!this::materials.isInitialized) {
            try {
                materials = getModels()
                    .map { it.modelDefinition }
                    .flatMap {
                        (0 until it.getFaceCount())
                            .map { face ->
                                it.getMaterial(face, cache)
                            }
                    }
                    .toSet()
                    .toTypedArray()
            } catch (e: Throwable) {
                Qodat.logException("Could not get entity {${getName()}}'s materials", e)
                return emptyArray()
            }
        }
        return materials
    }


    fun createMergedModel(name: String) = Model(
        name,
        if (definition.modelIds.size == 1)
            getModels().first().modelDefinition
        else
            getModels().map { it.modelDefinition }
                .toTypedArray()
                .let { RS2ModelBuilder(*it).build() },
        definition.findColor,
        definition.replaceColor
    )

    fun getDistinctModels() = if (!mergeModelProperty.get() || definition.modelIds.size == 1)
        getModels()
    else
        createDistinctModels()

    private fun createDistinctModels() = definition.modelIds.map {
        val modelDefinition = cache.getModelDefinition(it)
        Model(it, modelDefinition, definition.findColor, definition.replaceColor)
    }.toTypedArray()

    override fun getViewNode(): Node {
        if (!this::viewBox.isInitialized) {
            val optionalInt = definition.getOptionalId()
            val box = LabeledHBox(labelProperty)
            viewBox = if (optionalInt.isPresent)
                HBox().apply {
                    val id = optionalInt.asInt.toString()
                    val length = 7 - id.length
                    val spaces = Array(length) { "" }.joinToString(" ")
                    children.add(TextFlow().apply {
                        menloText("$id$spaces" to DEFAULT)
                    })
                    children.add(box)
                }
            else
                box
        }
        return viewBox
    }

    override fun getSceneNode(): Group {
        if (modelGroup == null) {
            modelGroup = Group().apply {
                for (model in getModels())
                    children.add(model.getSceneNode())
            }
        }
        return modelGroup!!
    }

    override fun removeSceneNodeReference() {
        modelGroup = null
        models?.forEach(Model::removeSceneNodeReference)
        models = null
    }

    override fun removeTreeItemReference() {
        treeItem = null
    }

    override fun getTreeItem(treeView: TreeView<Node>): EntityTreeItem {
        if (treeItem == null)
            treeItem = EntityTreeItem(this, treeView)
        return treeItem!!
    }

    override fun treeItemExpandedProperty(): BooleanProperty =
        Properties.treeItemEntityExpanded

    fun formatFileName(): String {
        val name = formatName().replace(" ", "_")
        val optionalId = definition.getOptionalId()
        return if (optionalId.isPresent)
            name + "_${optionalId.asInt}"
        else
            name
    }
}
