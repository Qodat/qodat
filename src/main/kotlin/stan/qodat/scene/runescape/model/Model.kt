package stan.qodat.scene.runescape.model

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.layout.HBox
import javafx.scene.shape.CullFace
import javafx.scene.shape.DrawMode
import javafx.scene.shape.MeshView
import stan.qodat.cache.Cache
import stan.qodat.cache.CacheEncoder
import stan.qodat.cache.definition.ModelDefinition
import stan.qodat.scene.control.LabeledHBox
import stan.qodat.scene.control.tree.ModelTreeItem
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.util.SceneNodeProvider
import stan.qodat.util.TreeItemProvider
import stan.qodat.util.ViewNodeProvider
import stan.qodat.util.onInvalidation
import java.io.UnsupportedEncodingException

/**
 * Represents a RuneScape 3D model.
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class Model(label: String,
            modelDefinition: ModelDefinition
) : ModelSkeleton(modelDefinition),
        ViewNodeProvider,
        SceneNodeProvider,
        TreeItemProvider,
        CacheEncoder {

    private lateinit var sceneGroup: Group
    private lateinit var sceneNode: Node
    private lateinit var modelMesh : ModelSkin
    private lateinit var viewBox : HBox
    private lateinit var treeItem: ModelTreeItem

    val labelProperty = SimpleStringProperty(label)
    val selectedProperty = SimpleBooleanProperty(false)
    val visibleProperty = SimpleBooleanProperty(true)
    val drawModeProperty = SimpleObjectProperty(DrawMode.FILL)
    val cullFaceProperty = SimpleObjectProperty(CullFace.NONE)
    val buildTypeProperty = SimpleObjectProperty(ModelMeshBuildType.SKELETON_ATLAS)

    init {
        buildTypeProperty.onInvalidation {
            if (this@Model::sceneNode.isInitialized)
                getSceneNode().children.remove(sceneNode)
            buildModelSkin()
            getSceneNode().children.add(sceneNode)
        }
        selectedProperty.onInvalidation { addOrRemoveSelectionBoxes(value) }
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

    fun collectMeshes() : Collection<ModelMesh> {
        return when (buildTypeProperty.get()!!){
            ModelMeshBuildType.ATLAS -> {
                listOf(modelMesh as ModelAtlasMesh)
            }
            ModelMeshBuildType.SKELETON_ATLAS -> {
                val atlasGroup = (modelMesh as ModelSkeletonMesh).getSceneNode()
                return atlasGroup.children.map {
                    (it as MeshView).mesh as ModelAtlasMesh
                }
            }
            ModelMeshBuildType.MESH_PER_FACE -> {
                val faceMeshGroup = (modelMesh as ModelFaceMeshGroup).getSceneNode()
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

    override fun animate(frame: AnimationFrame) {
        super.animate(frame)
        getModelSkin().updatePoints(this)
    }

    override fun getViewNode(): Node {
        if (!this::viewBox.isInitialized)
            viewBox = LabeledHBox(labelProperty)
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
        if (!this::modelMesh.isInitialized)
            buildModelSkin()
        return modelMesh
    }

    private fun buildModelSkin() {
        modelMesh = when (buildTypeProperty.get()!!) {
            ModelMeshBuildType.ATLAS -> ModelAtlasMesh(this)
            ModelMeshBuildType.SKELETON_ATLAS -> ModelSkeletonMesh(this)
            ModelMeshBuildType.MESH_PER_FACE -> ModelFaceMeshGroup(this)
        }
        sceneNode = modelMesh.getSceneNode()
    }

    override fun getTreeItem(treeView: TreeView<Node>): TreeItem<Node> {
        if (!this::treeItem.isInitialized)
            treeItem = ModelTreeItem(this, treeView.selectionModel)
        return treeItem
    }

    override fun encode(format: Cache) {
        throw UnsupportedEncodingException()
    }
}