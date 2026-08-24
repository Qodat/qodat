package stan.qodat.scene.runescape.model

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.EventHandler
import javafx.scene.DepthTest
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.CullFace
import javafx.scene.shape.DrawMode
import javafx.scene.shape.MeshView
import javafx.scene.shape.Sphere
import kotlinx.serialization.json.decodeFromStream
import mqo.MQOImporter
import stan.qodat.scene.control.export.blender.GltfCodec
import qodat.cache.Cache
import qodat.cache.EncodeResult
import qodat.cache.Encoder
import qodat.cache.definition.ModelDefinition
import qodat.cache.models.RSModelLoader
import stan.qodat.Properties
import stan.qodat.cache.impl.qodat.QodatCache
import stan.qodat.cache.impl.qodat.QodatModelDefinition
import stan.qodat.scene.control.LabeledHBox
import stan.qodat.scene.control.export.Exportable
import stan.qodat.scene.control.tree.ModelTreeItem
import stan.qodat.scene.provider.SceneNodeProvider
import stan.qodat.scene.provider.TreeItemProvider
import stan.qodat.scene.provider.ViewNodeProvider
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.util.PerfTrace
import stan.qodat.util.onInvalidation
import stan.qodat.util.setAndBind
import stan.qodat.util.unbindBidirectionalSafely
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
    Exportable,
    ViewNodeProvider,
    SceneNodeProvider,
    TreeItemProvider,
    Encoder {

    private var sceneGroup: Group? = null
    private lateinit var sceneNode: Node
    private lateinit var modelSkin : ModelSkin
    private lateinit var viewBox : HBox
    private var treeItem: ModelTreeItem? = null
    private lateinit var priorityLabels: Group
    private lateinit var vertexGroupMarkers: Group
    private lateinit var selectionHighlights: Group
    private val highlightRefCount = HashMap<Int, Int>()
    private val highlightSpheresByGroup = HashMap<Int, List<Node>>()

    private val overlayHoverHandler = EventHandler<MouseEvent> { event ->
        val node = event.source as? Node ?: return@EventHandler
        when (event.eventType) {
            MouseEvent.MOUSE_ENTERED, MouseEvent.MOUSE_MOVED -> {
                val text = overlayHoverText(node.userData) ?: return@EventHandler
                ModelOverlayHover.show(node, event, text)
            }
            MouseEvent.MOUSE_EXITED, MouseEvent.MOUSE_PRESSED -> ModelOverlayHover.hide()
        }
    }

    val labelProperty = SimpleStringProperty(label)
    val selectedProperty = SimpleBooleanProperty(false)
    val visibleProperty = SimpleBooleanProperty(true)
    val viewModeProperty = SimpleObjectProperty(ModelViewMode.FILL)
    val drawModeProperty = SimpleObjectProperty(DrawMode.FILL)
    val cullFaceProperty = SimpleObjectProperty(CullFace.NONE)
    val depthTestProperty = SimpleObjectProperty(DepthTest.ENABLE)
    val buildTypeProperty = SimpleObjectProperty(
        if (!Properties.alwaysRenderUsingAtlas.get() && modelDefinition.getFaceTextures() != null)
            ModelMeshBuildType.MESH_PER_FACE
        else
            ModelMeshBuildType.ATLAS
    )
    val displayFacePriorityLabelsProperty = SimpleBooleanProperty(false)
    val displayVertexGroupsProperty = SimpleBooleanProperty(false)
    val shadingProperty = SimpleBooleanProperty(false)
    val editProperty = SimpleObjectProperty<(ModelFaceMesh.EditContext.() -> Unit)?>(null)

    init {
        buildTypeProperty.onInvalidation {
            rebuildModel()
        }
        shadingProperty.onInvalidation {
            get()
            applyViewModeColors()
        }
        shadingProperty.setAndBind(Properties.shading, biDirectional = true)
        selectedProperty.onInvalidation { addOrRemoveSelectionBoxes(value) }
        displayFacePriorityLabelsProperty.onInvalidation { showOrHidePriorityLabels(value) }
        displayFacePriorityLabelsProperty.setAndBind(Properties.showPriorityLabels, biDirectional = true)
        displayVertexGroupsProperty.onInvalidation { showOrHideVertexGroupMarkers(value) }
        displayVertexGroupsProperty.setAndBind(Properties.showVertexGroups, biDirectional = true)
        viewModeProperty.onInvalidation {
            drawModeProperty.set(value.toDrawMode())
            applyViewModeColors()
        }
        viewModeProperty.setAndBind(Properties.viewMode, biDirectional = true)
        drawModeProperty.set(viewModeProperty.get().toDrawMode())
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
                for ((face, priority) in facePriorities.withIndex()) {
                    val center = getCenterPoint(face)
                    val marker = overlaySphere(distinctColor(priority.toInt() and 0xFF)).apply {
                        userData = OverlayMarker.FacePriority(face, priority.toInt() and 0xFF)
                        translateX = center.x
                        translateY = center.y
                        translateZ = center.z
                    }
                    priorityLabels.children.add(marker)
                }
            }
            if (!getSceneNode().children.contains(priorityLabels))
                getSceneNode().children.add(priorityLabels)
        } else if (this@Model::priorityLabels.isInitialized) {
            ModelOverlayHover.hide()
            getSceneNode().children.remove(priorityLabels)
        }
    }

    private fun showOrHideVertexGroupMarkers(value: Boolean) {
        if (value) {
            if (!this@Model::vertexGroupMarkers.isInitialized) {
                vertexGroupMarkers = Group()
                val skins = modelDefinition.getVertexSkins()
                if (skins != null) {
                    val counts = HashMap<Int, Int>()
                    for (skin in skins)
                        counts[skin] = (counts[skin] ?: 0) + 1
                    for (vertex in 0 until getVertexCount()) {
                        val skin = skins[vertex]
                        vertexGroupMarkers.children.add(vertexMarker(vertex, skin, counts[skin] ?: 0))
                    }
                } else {
                    for ((groupIndex, vertices) in getVertexGroups().withIndex()) {
                        for (vertex in vertices)
                            vertexGroupMarkers.children.add(vertexMarker(vertex, groupIndex, vertices.size))
                    }
                }
            }
            if (!getSceneNode().children.contains(vertexGroupMarkers))
                getSceneNode().children.add(vertexGroupMarkers)
        } else if (this@Model::vertexGroupMarkers.isInitialized) {
            ModelOverlayHover.hide()
            getSceneNode().children.remove(vertexGroupMarkers)
        }
    }

    fun addVertexGroupHighlight(groupIndex: Int) =
        addVertexGroupHighlights(listOf(groupIndex))

    fun removeVertexGroupHighlight(groupIndex: Int) =
        removeVertexGroupHighlights(listOf(groupIndex))

    fun addVertexGroupHighlights(groupIndices: Collection<Int>) {
        for (groupIndex in groupIndices.distinct()) {
            val refs = (highlightRefCount[groupIndex] ?: 0) + 1
            highlightRefCount[groupIndex] = refs
            if (refs == 1)
                addHighlightSpheres(groupIndex)
        }
        if (highlightRefCount.isNotEmpty()) {
            val group = ensureSelectionHighlights()
            if (!getSceneNode().children.contains(group))
                getSceneNode().children.add(group)
        }
    }

    fun removeVertexGroupHighlights(groupIndices: Collection<Int>) {
        if (!this::selectionHighlights.isInitialized)
            return
        for (groupIndex in groupIndices.distinct()) {
            val refs = (highlightRefCount[groupIndex] ?: 0) - 1
            if (refs <= 0) {
                highlightRefCount.remove(groupIndex)
                val spheres = highlightSpheresByGroup.remove(groupIndex) ?: continue
                selectionHighlights.children.removeAll(spheres)
            } else {
                highlightRefCount[groupIndex] = refs
            }
        }
        if (highlightRefCount.isEmpty())
            getSceneNode().children.remove(selectionHighlights)
    }

    fun clearVertexGroupHighlights() {
        if (!this::selectionHighlights.isInitialized)
            return
        highlightRefCount.clear()
        highlightSpheresByGroup.clear()
        selectionHighlights.children.clear()
        getSceneNode().children.remove(selectionHighlights)
    }

    private fun addHighlightSpheres(groupIndex: Int) {
        val vertices = getVertexGroups().getOrNull(groupIndex) ?: return
        if (vertices.isEmpty())
            return
        val spheres = vertices.map { vertex ->
            overlaySphere(highlightColor(groupIndex), HIGHLIGHT_MARKER_RADIUS).apply {
                userData = OverlayMarker.Vertex(vertex, groupIndex, vertices.size)
                translateX = getX(vertex).toDouble()
                translateY = getY(vertex).toDouble()
                translateZ = getZ(vertex).toDouble()
            }
        }
        highlightSpheresByGroup[groupIndex] = spheres
        ensureSelectionHighlights().children.addAll(spheres)
    }

    private fun ensureSelectionHighlights(): Group {
        if (!this::selectionHighlights.isInitialized)
            selectionHighlights = Group()
        return selectionHighlights
    }

    private fun highlightColor(group: Int): Color =
        distinctColor(group).deriveColor(0.0, 0.85, 1.45, 1.0)

    private fun vertexMarker(vertex: Int, skin: Int, count: Int): Sphere =
        overlaySphere(distinctColor(skin)).apply {
            userData = OverlayMarker.Vertex(vertex, skin, count)
            translateX = getX(vertex).toDouble()
            translateY = getY(vertex).toDouble()
            translateZ = getZ(vertex).toDouble()
        }

    private fun overlaySphere(color: Color, radius: Double = OVERLAY_MARKER_RADIUS): Sphere =
        Sphere(radius, OVERLAY_MARKER_DIVISIONS).apply {
            material = PhongMaterial(color)
            depthTest = DepthTest.DISABLE
            cullFace = CullFace.NONE
            isMouseTransparent = false
            isPickOnBounds = false
            addEventHandler(MouseEvent.MOUSE_ENTERED, overlayHoverHandler)
            addEventHandler(MouseEvent.MOUSE_MOVED, overlayHoverHandler)
            addEventHandler(MouseEvent.MOUSE_EXITED, overlayHoverHandler)
            addEventHandler(MouseEvent.MOUSE_PRESSED, overlayHoverHandler)
        }

    private fun overlayHoverText(data: Any?): String? = when (data) {
        is OverlayMarker.Vertex ->
            "VERTEX GROUP ${data.group}  ·  vertex #${data.vertex}  ·  ${data.count} vertices"
        is OverlayMarker.FacePriority ->
            "PRIORITY ${data.priority}  ·  face #${data.face}"
        else -> null
    }

    internal fun hoverTextForFace(face: Int): String? {
        val definition = modelDefinition
        return when (viewModeProperty.get() ?: return null) {
            ModelViewMode.VERTEX_SKIN -> {
                val skins = definition.getVertexSkins() ?: return null
                val v1 = definition.getFaceVertexIndices1()[face]
                val v2 = definition.getFaceVertexIndices2()[face]
                val v3 = definition.getFaceVertexIndices3()[face]
                val s1 = skins[v1]
                val s2 = skins[v2]
                val s3 = skins[v3]
                if (s1 == s2 && s2 == s3)
                    "VERTEX GROUP $s1  ·  face #$face  ·  ${vertexGroupSize(s1)} vertices"
                else
                    "VERTEX GROUPS $s1 / $s2 / $s3  ·  face #$face"
            }
            ModelViewMode.FACE_SKIN -> {
                val skin = definition.getFaceSkins()?.get(face) ?: return null
                "FACE GROUP $skin  ·  face #$face  ·  ${faceGroupSize(skin)} faces"
            }
            ModelViewMode.PRIORITY -> {
                val priority = (definition.getFacePriorities()?.get(face) ?: definition.getPriority()).toInt() and 0xFF
                "PRIORITY $priority  ·  face #$face"
            }
            ModelViewMode.FILL, ModelViewMode.LINE -> null
        }
    }

    private fun vertexGroupSize(group: Int): Int {
        val groups = getVertexGroups()
        if (group in groups.indices)
            return groups[group].size
        return modelDefinition.getVertexSkins()?.count { it == group } ?: 0
    }

    private fun faceGroupSize(group: Int): Int {
        val groups = getFaceGroups()
        if (group in groups.indices)
            return groups[group].size
        return modelDefinition.getFaceSkins()?.count { it == group } ?: 0
    }

    private fun updateOverlayPositions() {
        if (this::vertexGroupMarkers.isInitialized && vertexGroupMarkers.parent != null) {
            for (child in vertexGroupMarkers.children) {
                val vertex = (child.userData as? OverlayMarker.Vertex)?.vertex ?: continue
                child.translateX = getX(vertex).toDouble()
                child.translateY = getY(vertex).toDouble()
                child.translateZ = getZ(vertex).toDouble()
            }
        }
        if (this::priorityLabels.isInitialized && priorityLabels.parent != null) {
            for (child in priorityLabels.children) {
                val face = (child.userData as? OverlayMarker.FacePriority)?.face ?: continue
                val center = getCenterPoint(face)
                child.translateX = center.x
                child.translateY = center.y
                child.translateZ = center.z
            }
        }
        if (this::selectionHighlights.isInitialized && selectionHighlights.parent != null) {
            for (child in selectionHighlights.children) {
                val vertex = (child.userData as? OverlayMarker.Vertex)?.vertex ?: continue
                child.translateX = getX(vertex).toDouble()
                child.translateY = getY(vertex).toDouble()
                child.translateZ = getZ(vertex).toDouble()
            }
        }
    }

    fun collectMeshes() : Collection<ModelMesh> {
        return when (buildTypeProperty.get()!!){
            ModelMeshBuildType.ATLAS -> {
                listOf(modelSkin as ModelAtlasMesh)
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
        if (clearAnimatedAlphas())
            applyViewModeColors()
        updateOverlayPositions()
    }

    fun recolor() {
        applyViewModeColors()
    }

    private fun applyViewModeColors() {
        ModelOverlayHover.hide()
        if (!this::modelSkin.isInitialized)
            return
        when (val skin = modelSkin) {
            is ModelAtlasMesh -> skin.rebuildAtlas()
            is ModelSkeletonMesh -> {
                for (child in skin.getSceneNode().children) {
                    ((child as? MeshView)?.mesh as? ModelAtlasMesh)?.rebuildAtlas()
                }
            }
            // Per face meshes bake their colour into a material each, so there is nothing to
            // recolour in place.
            is ModelFaceMeshGroup -> rebuildModel()
        }
    }

    override fun animate(frame: AnimationFrame) {
        super.animate(frame)
        getModelSkin().updatePoints(this)
        if (pullAlphaChanged())
            applyViewModeColors()
        updateOverlayPositions()
    }

    override fun getViewNode(): Node {
        if (!this::viewBox.isInitialized) {
            viewBox = LabeledHBox(labelProperty, labelPrefix = "model")
        }
        return viewBox
    }

    override fun getSceneNode() : Group {
        if (sceneGroup == null){
            sceneGroup = Group().apply {
                if (!this@Model::sceneNode.isInitialized)
                    buildModelSkin()
                children.add(sceneNode)
            }
        }
        return sceneGroup!!
    }

    override fun removeSceneNodeReference() {
        unbindGlobalProperties()
        disposeOverlays()
        sceneGroup = null
    }

    override fun removeTreeItemReference() {
        treeItem = null
    }

    private fun unbindGlobalProperties() {
        shadingProperty.unbindBidirectionalSafely(Properties.shading)
        displayFacePriorityLabelsProperty.unbindBidirectionalSafely(Properties.showPriorityLabels)
        displayVertexGroupsProperty.unbindBidirectionalSafely(Properties.showVertexGroups)
        viewModeProperty.unbindBidirectionalSafely(Properties.viewMode)
    }

    private fun disposeOverlays() {
        ModelOverlayHover.hide()
        highlightRefCount.clear()
        highlightSpheresByGroup.clear()
        if (this::vertexGroupMarkers.isInitialized)
            vertexGroupMarkers.children.clear()
        if (this::priorityLabels.isInitialized)
            priorityLabels.children.clear()
        if (this::selectionHighlights.isInitialized)
            selectionHighlights.children.clear()
    }

    private fun getModelSkin() : ModelSkin {
        if (!this::modelSkin.isInitialized)
            buildModelSkin()
        return modelSkin
    }

    private fun buildModelSkin() {
        PerfTrace.span("model.buildSkin ${labelProperty.get()}") {
            modelSkin = when (buildTypeProperty.get()!!) {
                ModelMeshBuildType.ATLAS -> ModelAtlasMesh(this)
                ModelMeshBuildType.SKELETON_ATLAS -> ModelSkeletonMesh(this)
                ModelMeshBuildType.MESH_PER_FACE -> ModelFaceMeshGroup(this)
            }
            sceneNode = modelSkin.getSceneNode()
        }
    }

    override fun treeItemExpandedProperty(): BooleanProperty =
        Properties.treeItemEntityExpanded

    override fun getTreeItem(treeView: TreeView<Node>): TreeItem<Node> {
        if (treeItem == null)
            treeItem = ModelTreeItem(this, treeView.selectionModel)
        return treeItem!!
    }

    override fun encode(format: Cache) : EncodeResult =
        format.encode(this)

    override fun getName(): String =
        labelProperty.get()

    companion object {

        private const val OVERLAY_MARKER_RADIUS = 3.0
        private const val HIGHLIGHT_MARKER_RADIUS = 4.5

        // One marker per vertex/face, so keep the sphere tessellation far below
        // the JavaFX default of 64 divisions.
        private const val OVERLAY_MARKER_DIVISIONS = 6

        val supportedExtensions = arrayOf("model", "dat", "json", "mqo", "glb")

        fun fromFile(file: File) : Model {
            val definition = when (file.extension.lowercase()) {
                "json" -> {
                    QodatCache.json.decodeFromStream<QodatModelDefinition>(file.inputStream())
                }
                "mqo" -> {
                    MQOImporter().load(file)
                }
                "glb" -> {
                    GltfCodec.read(file.toPath())
                }
                else -> {
                    // TODO: support gzip, mqo
                    RSModelLoader().load(file.nameWithoutExtension, file.readBytes())
                }
            }
            return Model(definition.getName(), definition)
        }
    }

    private sealed class OverlayMarker {
        data class Vertex(val vertex: Int, val group: Int, val count: Int) : OverlayMarker()
        data class FacePriority(val face: Int, val priority: Int) : OverlayMarker()
    }
}
