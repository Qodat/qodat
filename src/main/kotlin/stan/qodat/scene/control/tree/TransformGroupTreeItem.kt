package stan.qodat.scene.control.tree

import javafx.event.EventHandler
import javafx.scene.DepthTest
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.CullFace
import javafx.scene.shape.DrawMode
import javafx.scene.shape.Sphere
import stan.qodat.Properties
import stan.qodat.javafx.label
import stan.qodat.javafx.onExpanded
import stan.qodat.javafx.onTreeSelected
import stan.qodat.javafx.text
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.control.gizmo.GizmoMath
import stan.qodat.scene.control.gizmo.TransformGizmo
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.animation.Transformation
import stan.qodat.scene.runescape.animation.TransformationType
import stan.qodat.scene.runescape.entity.AnimatedEntity
import stan.qodat.scene.runescape.model.Model
import kotlin.math.abs

class TransformGroupTreeItem(
    private val entity: AnimatedEntity<*>,
    frame: AnimationFrame,
    frameItem: TreeItem<Node>,
    treeView: TreeView<Node>,
    private val rootTransformation: Transformation,
    private val childTransformations: List<Transformation>
) : TreeItem<Node>(), LineMode {

    init {
        text("TRANSFORMATIONS", Color.web("#FFC66D"))
        label("${childTransformations.size}")
        val root = TransformTreeItem(entity, frame, rootTransformation, frameItem, treeView.selectionModel)
        children.add(root)
        onExpanded {
            root.children.clear()
            for ((index, transform) in childTransformations.withIndex())
                root.children.add(
                    index,
                    TransformTreeItem(entity, frame, transform, root, treeView.selectionModel))
        }
        onTreeSelected(treeView.selectionModel) { oldValue, newValue ->
            if (newValue == this) {
                entity.getSceneNode().children.addAll(getSelectionMesh())
                selectGizmo()
            } else if (oldValue == this) {
                entity.getSceneNode().children.removeAll(selectionMesh)
                unselectGizmo()
            }
        }
    }

    private lateinit var selectionMesh: Group

    val gizmo by lazy {
        TransformGizmo().also { widget ->
            widget.onTranslate = { dx, dy, dz ->
                childTransformations.find { it.getType() == TransformationType.TRANSLATE }?.let { transform ->
                    transform.deltaXProperty.set(transform.getDeltaX() + dx.toInt())
                    transform.deltaYProperty.set(transform.getDeltaY() + dy.toInt())
                    transform.deltaZProperty.set(transform.getDeltaZ() + dz.toInt())
                }
            }
            widget.onRotate = { degX, degY, degZ ->
                childTransformations.find { it.getType() == TransformationType.ROTATE }?.let { transform ->
                    transform.deltaXProperty.set(transform.getDeltaX() + GizmoMath.degreesToRs(degX))
                    transform.deltaYProperty.set(transform.getDeltaY() + GizmoMath.degreesToRs(degY))
                    transform.deltaZProperty.set(transform.getDeltaZ() + GizmoMath.degreesToRs(degZ))
                }
            }
        }
    }

    fun selectGizmo(){
        if (Properties.posingMode.get())
            return
        SubScene3D.mouseListener.set(EventHandler { event ->
            val parent = gizmo.parent
            when (event.eventType) {
                javafx.scene.input.MouseEvent.MOUSE_PRESSED -> {
                    if (parent == null)
                        return@EventHandler
                    val hit = gizmo.pickScreen(parent, event.screenX, event.screenY) ?: return@EventHandler
                    gizmo.beginDrag(hit.first, hit.second)
                    event.consume()
                }
                javafx.scene.input.MouseEvent.MOUSE_DRAGGED -> {
                    if (!gizmo.isDragging() || parent == null)
                        return@EventHandler
                    gizmo.dragFromMouse(parent, SubScene3D.cameraHandler.camera, event)
                    event.consume()
                }
                javafx.scene.input.MouseEvent.MOUSE_RELEASED,
                javafx.scene.input.MouseEvent.MOUSE_CLICKED -> {
                    if (!gizmo.isDragging())
                        return@EventHandler
                    gizmo.endDrag()
                    event.consume()
                }
            }
        })
        if (!getSelectionMesh().children.contains(gizmo))
            getSelectionMesh().children.add(gizmo)
    }

    fun unselectGizmo(){
        if (SubScene3D.mouseListener.get() != null && !Properties.posingMode.get())
            SubScene3D.mouseListener.set(null)
        gizmo.endDrag()
        getSelectionMesh().children.remove(gizmo)
    }

    fun getSelectionMesh(): Group {
        if (!this::selectionMesh.isInitialized) {
            selectionMesh = Group()
            var meshId = 0
            var totalX = 0.0
            var totalY = 0.0
            var totalZ = 0.0


            for (i in 0 until rootTransformation.groupIndices.size()) {
                val groupIndex = rootTransformation.groupIndices[i]
                for (model in entity.getModels()) {
                    val vertices = model.getVertexGroups().getOrNull(groupIndex) ?: continue
                    val sphere = getSelectionBox(meshId++, model, vertices)
                    totalX += sphere.translateX
                    totalY += sphere.translateY
                    totalZ += sphere.translateZ
                    selectionMesh.children.add(sphere)
                }
            }
            gizmo.translateX = totalX / meshId
            gizmo.translateY = totalY / meshId
            gizmo.translateZ = totalZ / meshId
        }
        return selectionMesh
    }

    private fun getSelectionBox(meshId: Int, model: Model, vertices: IntArray) : Sphere {
        val selectionBox = Sphere()
        selectionBox.id = "$meshId"
        selectionBox.cullFace = CullFace.FRONT
        selectionBox.drawMode = DrawMode.FILL
        selectionBox.depthTest = DepthTest.DISABLE
        selectionBox.material = PhongMaterial(Color.web("#CC7832"))
        computerCenter(selectionBox, model, vertices)
        return selectionBox
    }

    private fun computerCenter(sphere: Sphere, model: Model, vertices: IntArray) {
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var minZ = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        var maxZ = Int.MIN_VALUE
        for (vertex in vertices) {
            val x = model.getX(vertex)
            val y = model.getY(vertex)
            val z = model.getZ(vertex)
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
            if (z < minZ) minZ = z
            if (z > maxZ) maxZ = z
        }
        val width = abs(maxX - minX)
        val height = abs(maxY - minY)
        val depth = abs(maxZ - minZ)
        sphere.radius = 3.0
        sphere.translateX = (minX + width.toDouble().div(2))
        sphere.translateY = (minY + height.toDouble().div(2))
        sphere.translateZ = (minZ + depth.toDouble().div(2))
    }
}