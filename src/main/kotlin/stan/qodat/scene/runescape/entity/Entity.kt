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
 * JavaFX properties are created on first access so wrapping tens of thousands
 * of cache entities does not allocate property objects per row.
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
    private var preparedDefinitions: List<Pair<String, ModelDefinition>>? = null
    private var preparedMerged: ModelDefinition? = null
    private val modelLock = Any()
    private lateinit var materials: Array<Material>
    private lateinit var viewBox: HBox
    private var treeItem: EntityTreeItem? = null

    private var lockedProp: SimpleBooleanProperty? = null
    private var labelProp: SimpleStringProperty? = null
    private var mergeModelProp: SimpleBooleanProperty? = null

    val locked: SimpleBooleanProperty
        get() = lockedProp ?: SimpleBooleanProperty(false).also { prop ->
            lockedProp = prop
            prop.addListener { _, oldValue, newValue ->
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
    val labelProperty: SimpleStringProperty
        get() = labelProp ?: SimpleStringProperty(definition.name).also { labelProp = it }
    val mergeModelProperty: SimpleBooleanProperty
        get() = mergeModelProp ?: SimpleBooleanProperty(true).also { mergeModelProp = it }

    abstract fun property(): SimpleStringProperty

    override fun getName() = labelProp?.get() ?: definition.name

    /**
     * Decode and merge cache models without creating JavaFX [Group]/[Mesh] nodes.
     * Safe on Default/IO; [getSceneNode] still applies the scene graph on the FX thread.
     */
    fun prepareModels() {
        synchronized(modelLock) {
            if (models != null || preparedDefinitions != null)
                return
            try {
                val loaded = loadModelDefinitions()
                preparedDefinitions = loaded
                if (loaded.size > 1 && mergeModelsEnabled()) {
                    preparedMerged = PerfTrace.span("entity.mergeModels ${getName()}") {
                        RS2ModelBuilder(*loaded.map { it.second }.toTypedArray()).build()
                    }
                }
            } catch (e: Throwable) {
                Qodat.logException("Could not prepare entity {${getName()}}'s models", e)
                preparedDefinitions = emptyList()
            }
        }
    }

    fun getModels(): Array<Model> {
        models?.let { return it }
        synchronized(modelLock) {
            models?.let { return it }
            models = PerfTrace.span("entity.getModels ${getName()}") {
                try {
                    wrapPreparedModels()
                } catch (e: Throwable) {
                    Qodat.logException("Could not get entity {${getName()}}'s models", e)
                    emptyArray()
                }
            }
            return models!!
        }
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

    private fun loadModelDefinitions(): List<Pair<String, ModelDefinition>> =
        definition.modelIds.mapNotNull { modelId ->
            loadModelDefinition(modelId)?.let { modelId to it }
        }

    private fun wrapPreparedModels(): Array<Model> {
        val loaded = preparedDefinitions ?: loadModelDefinitions().also { preparedDefinitions = it }
        if (loaded.isEmpty())
            return emptyArray()
        val merged = preparedMerged
        return if (merged != null) {
            val multiModelName = "models_${loaded.joinToString { it.second.getName() + "_" }}"
            arrayOf(Model(multiModelName, merged, definition.findColor, definition.replaceColor))
        } else if (loaded.size > 1 && mergeModelsEnabled()) {
            PerfTrace.span("entity.mergeModels ${getName()}") {
                val modelDefinition = RS2ModelBuilder(*loaded.map { it.second }.toTypedArray()).build()
                preparedMerged = modelDefinition
                val multiModelName = "models_${loaded.joinToString { it.second.getName() + "_" }}"
                arrayOf(Model(multiModelName, modelDefinition, definition.findColor, definition.replaceColor))
            }
        } else
            loaded.map { (id, def) -> modelFromDefinition(id, def) }.toTypedArray()
    }

    fun getMaterials(): Array<Material> {
        if (!this::materials.isInitialized) {
            materials = PerfTrace.span("entity.getMaterials ${getName()}") {
                try {
                    uniqueMaterials()
                } catch (e: Throwable) {
                    Qodat.logException("Could not get entity {${getName()}}'s materials", e)
                    emptyArray()
                }
            }
        }
        return materials
    }

    private fun mergeModelsEnabled(): Boolean = mergeModelProp?.get() ?: true

    private fun uniqueMaterials(): Array<Material> {
        val out = LinkedHashMap<Long, Material>()
        for (model in getModels()) {
            val modelDefinition = model.modelDefinition
            val colors = modelDefinition.getFaceColors()
            val alphas = modelDefinition.getFaceAlphas()
            val textures = modelDefinition.getFaceTextures()
            for (face in 0 until modelDefinition.getFaceCount()) {
                val textureId = textures?.getOrNull(face)?.toInt() ?: -1
                val key = if (textureId != -1) {
                    1L shl 32 or (textureId.toLong() and 0xffffffffL)
                } else {
                    val color = colors[face].toInt() and 0xffff
                    val alpha = (alphas?.getOrNull(face)?.toInt() ?: 256) and 0xffff
                    color.toLong() shl 16 or alpha.toLong()
                }
                if (key !in out) {
                    out[key] = modelDefinition.getMaterial(face, cache)
                }
            }
        }
        return out.values.toTypedArray()
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

    fun getDistinctModels() = if (!mergeModelsEnabled() || definition.modelIds.size == 1)
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
        preparedDefinitions = null
        preparedMerged = null
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
