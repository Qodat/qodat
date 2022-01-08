package stan.qodat.scene.runescape.model

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.layout.HBox
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.CullFace
import javafx.scene.shape.DrawMode
import javafx.scene.shape.MeshView
import javafx.scene.shape.Sphere
import kotlinx.serialization.json.decodeFromStream
import stan.qodat.Properties
import stan.qodat.cache.Cache
import stan.qodat.cache.EncodeResult
import stan.qodat.cache.Encoder
import stan.qodat.cache.definition.ModelDefinition
import stan.qodat.cache.impl.qodat.QodatCache
import stan.qodat.cache.impl.qodat.QodatModelDefinition
import stan.qodat.cache.util.RSModelLoader
import stan.qodat.scene.control.LabeledHBox
import stan.qodat.scene.control.tree.ModelTreeItem
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.util.*
import java.io.File

/**
 * Represents a RuneScape 3D model.
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class Model(label: String,
            modelDefinition: ModelDefinition,
            internal val findColor: ShortArray? = null,
            internal val replaceColor: ShortArray? = null
) : ModelSkeleton(modelDefinition),
        Searchable,
        ViewNodeProvider,
        SceneNodeProvider,
        TreeItemProvider,
        Encoder {

    private lateinit var sceneGroup: Group
    private lateinit var sceneNode: Node
    private lateinit var modelSkin : ModelSkin
    private lateinit var viewBox : HBox
    private lateinit var treeItem: ModelTreeItem
    private lateinit var priorityLabels: Group

    val labelProperty = SimpleStringProperty(label)
    val selectedProperty = SimpleBooleanProperty(false)
    val visibleProperty = SimpleBooleanProperty(true)
    val drawModeProperty = SimpleObjectProperty(DrawMode.FILL)
    val cullFaceProperty = SimpleObjectProperty(CullFace.NONE)
    val buildTypeProperty = SimpleObjectProperty(ModelMeshBuildType.ATLAS)
    val displayFacePriorityLabelsProperty = SimpleBooleanProperty(false)
    val shadingProperty = SimpleBooleanProperty(false)
    val editProperty = SimpleObjectProperty<(ModelFaceMesh.EditContext.() -> Unit)?>(null)

    init {
        buildTypeProperty.onInvalidation {
            rebuildModel()
        }
        shadingProperty.onInvalidation {
            buildTypeProperty.set(if (value) ModelMeshBuildType.TEXTURED_ATLAS else ModelMeshBuildType.ATLAS)
        }
        selectedProperty.onInvalidation { addOrRemoveSelectionBoxes(value) }
        displayFacePriorityLabelsProperty.onInvalidation { showOrHidePriorityLabels(value) }
        displayFacePriorityLabelsProperty.setAndBind(Properties.showPriorityLabels, biDirectional = true)
        editProperty.onInvalidation {
            buildTypeProperty.set(ModelMeshBuildType.MESH_PER_FACE)
        }

    }

    private fun rebuildModel() {
        if (this@Model::sceneNode.isInitialized)
            getSceneNode().children.remove(sceneNode)
        buildModelSkin()
        getSceneNode().children.add(sceneNode)
    }

    private fun addOrRemoveSelectionBoxes(add: Boolean) {
        val group = getSceneNode()
        val meshes = collectMeshes()
        if (add) {
            for (mesh in meshes)
                group.children.add(mesh.getSelectionBox())
        } else {
            for (mesh in meshes)
                group.children.remove(mesh.getSelectionBox())
        }
    }

    private fun showOrHidePriorityLabels(value: Boolean) {
        if (value) {
            if (!this@Model::priorityLabels.isInitialized) {
                priorityLabels = Group()
                val facePriorities = modelDefinition.getFacePriorities()
                    ?: ByteArray(modelDefinition.getFaceCount())
                    { modelDefinition.getPriority() }
                // TODO: disable for SkeletonMesh?
                for ((face, priority) in facePriorities.withIndex()) {
                    val center = getCenterPoint(face)
                    val circle = Sphere().apply {
                        material = PhongMaterial(DISTINCT_COLORS[priority.toInt()])
                        translateX = center.x
                        translateY = center.y
                        translateZ = center.z - 60.0
                    }
//                    val text = Text3D(priority.toString(),  Font.font(6.0)).apply {
//                        depthTest = DepthTest.DISABLE
//                        translateX = center.x
//                        translateY = center.y
//                        translateZ = center.z - 60.0
//                    }
                    priorityLabels.children.add(circle)
                }
            }
            if (!getSceneNode().children.contains(priorityLabels))
                getSceneNode().children.add(priorityLabels)
        } else if (this@Model::priorityLabels.isInitialized)
            getSceneNode().children.remove(priorityLabels)
    }

    fun collectMeshes() : Collection<ModelMesh> {
        return when (buildTypeProperty.get()!!){
            ModelMeshBuildType.ATLAS -> {
                listOf(modelSkin as ModelAtlasMesh)
            }
            ModelMeshBuildType.TEXTURED_ATLAS -> {
                emptyList()
            }
            ModelMeshBuildType.SKELETON_ATLAS -> {
                val atlasGroup = (modelSkin as ModelSkeletonMesh).getSceneNode()
                return atlasGroup.children.map {
                    (it as MeshView).mesh as ModelAtlasMesh
                }
            }
            ModelMeshBuildType.MESH_PER_FACE -> {
                val faceMeshGroup = (modelSkin as ModelFaceMeshGroup).getSceneNode()
                return faceMeshGroup.children.map {
                    (it as MeshView).mesh as ModelFaceMesh
                }
            }
        }
    }

    fun reset(){
        copyOriginalVertexValues()
        getModelSkin().updatePoints(this)
    }

    fun recolor() {
        when (val skin = modelSkin) {
            is ModelAtlasMesh -> {
                skin.rebuildAtlas()
            }
            else -> throw Exception("Recoloring is not supported for this skin $skin")
        }
    }

    override fun animate(frame: AnimationFrame) {
        super.animate(frame)
        getModelSkin().updatePoints(this)
    }

    override fun getViewNode(): Node {
        if (!this::viewBox.isInitialized) {
            viewBox = LabeledHBox(labelProperty)
        }
        return viewBox
    }

    override fun getSceneNode() : Group {
        if (!this::sceneGroup.isInitialized){
            sceneGroup = Group()
            if (!this::sceneNode.isInitialized)
                buildModelSkin()
            sceneGroup.children.add(sceneNode)
        }
        return sceneGroup
    }

    private fun getModelSkin() : ModelSkin {
        if (!this::modelSkin.isInitialized)
            buildModelSkin()
        return modelSkin
    }

    private fun buildModelSkin() {
        modelSkin = when (buildTypeProperty.get()!!) {
            ModelMeshBuildType.ATLAS -> ModelAtlasMesh(this)
            ModelMeshBuildType.TEXTURED_ATLAS -> ModelTexturedMesh(this, modelSkin as ModelAtlasMesh)
            ModelMeshBuildType.SKELETON_ATLAS -> ModelSkeletonMesh(this)
            ModelMeshBuildType.MESH_PER_FACE -> ModelFaceMeshGroup(this)
        }
        sceneNode = modelSkin.getSceneNode()
    }

    override fun getTreeItem(treeView: TreeView<Node>): TreeItem<Node> {
        if (!this::treeItem.isInitialized)
            treeItem = ModelTreeItem(this, treeView.selectionModel)
        return treeItem
    }

    override fun encode(format: Cache) : EncodeResult {
        return format.encode(this)
    }

    override fun getName() = labelProperty.get()


    companion object {

        val supportedExtensions = arrayOf("model", "dat", "json")

        fun toFile(file: File) : Model {
            val definition = when (file.extension) {
                "json" -> {
                    QodatCache.json.decodeFromStream<QodatModelDefinition>(file.inputStream())
                }
                else -> {
                    // TODO: support gzip, mqo
                    RSModelLoader().load(file.nameWithoutExtension, file.readBytes())
                }
            }
            return Model(definition.getName(), definition)
        }

        fun fromFile(file: File) : Model {
            val definition = when (file.extension) {
                "json" -> {
                    QodatCache.json.decodeFromStream<QodatModelDefinition>(file.inputStream())
                }
                else -> {
                    // TODO: support gzip, mqo
                    RSModelLoader().load(file.nameWithoutExtension, file.readBytes())
                }
            }
            return Model(definition.getName(), definition)
        }
    }
}