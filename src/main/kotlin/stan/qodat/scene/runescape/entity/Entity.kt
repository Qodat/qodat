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
import qodat.cache.definition.ModelDefinition
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
import stan.qodat.util.PerfTrace
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
    val definition: D,
    val labelPrefix: String? = null
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
            models = PerfTrace.span("entity.getModels ${getName()}") {
                try {
                    loadModels()
                } catch (e: Throwable) {
                    Qodat.logException("Could not get entity {${getName()}}'s models", e)
                    emptyArray()
                }
            }
        }
        return models!!
    }

    private fun loadModelDefinition(modelId: String) = try {
        PerfTrace.span("cache.model $modelId") {
            cache.getModelDefinition(modelId)
        }
    } catch (e: Throwable) {
        Qodat.logException("Could not load model $modelId for entity {${getName()}}", e)
        null
    }

    private fun modelFromDefinition(modelId: String, modelDefinition: ModelDefinition) =
        Model(modelId, modelDefinition, definition.findColor, definition.replaceColor)

    private fun loadModels(): Array<Model> {
        val loaded = definition.modelIds.mapNotNull { modelId ->
            loadModelDefinition(modelId)?.let { modelId to it }
        }
        if (loaded.isEmpty())
            return emptyArray()
        return if (loaded.size > 1 && mergeModelProperty.get()) {
            PerfTrace.span("entity.mergeModels ${getName()}") {
                val definitions = loaded.map { it.second }.toTypedArray()
                val multiModelName = "models_${
                    definitions.joinToString {
                        it.getName() + "_"
                    }
                }"
                val modelDefinition = RS2ModelBuilder(*definitions).build()
                arrayOf(Model(multiModelName, modelDefinition, definition.findColor, definition.replaceColor))
            }
        } else
            loaded.map { (id, def) -> modelFromDefinition(id, def) }.toTypedArray()
    }

    fun getMaterials(): Array<Material> {
        if (!this::materials.isInitialized) {
            materials = PerfTrace.span("entity.getMaterials ${getName()}") {
                try {
                    // TODO(perf): unique-by texture/color id; getMaterial allocates Texture/ColorMaterial per face.
                    getModels()
                        .map { it.modelDefinition }
                        .flatMap { definition ->
                            (0 until definition.getFaceCount()).map { face ->
                                definition.getMaterial(face, cache)
                            }
                        }
                        .toSet()
                        .toTypedArray()
                } catch (e: Throwable) {
                    Qodat.logException("Could not get entity {${getName()}}'s materials", e)
                    emptyArray()
                }
            }
        }
        return materials
    }

    fun getRecolorMap(): Map<Short, Short>? = definition.let {
        it.findColor?.mapIndexed { index, toFind -> toFind to it.replaceColor!![index] }?.toMap()
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

    private fun createDistinctModels() = definition.modelIds.mapNotNull { modelId ->
        loadModelDefinition(modelId)?.let { modelFromDefinition(modelId, it) }
    }.toTypedArray()

    override fun getViewNode(): Node {
        if (!this::viewBox.isInitialized) {
            val optionalInt = definition.getOptionalId()
            val box = LabeledHBox(labelProperty, labelPrefix = labelPrefix)
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
            modelGroup = PerfTrace.span("entity.sceneNode ${getName()}") {
                Group().apply {
                    for (model in getModels())
                        children.add(model.getSceneNode())
                }
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
        models?.forEach { it.removeTreeItemReference() }
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
