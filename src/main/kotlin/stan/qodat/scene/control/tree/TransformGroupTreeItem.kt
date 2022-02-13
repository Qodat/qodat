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
import org.joml.Vector3f
import stan.qodat.scene.control.gizmo.GizmoStackoverflow
import stan.qodat.javafx.label
import stan.qodat.javafx.onExpanded
import stan.qodat.javafx.onSelected
import stan.qodat.javafx.text
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.control.gizmo.toRay
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.animation.Transformation
import stan.qodat.scene.runescape.animation.TransformationType
import stan.qodat.scene.runescape.entity.AnimatedEntity
import stan.qodat.scene.runescape.model.Model
import stan.qodat.util.setAndBind
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
        treeView.selectionModel.onSelected { oldValue, newValue ->
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
        val gizmo = GizmoStackoverflow.Gizmo()

        val controller = gizmo.controller

        childTransformations.forEach {
            when (it.getType()) {
                TransformationType.TRANSLATE -> {
                    controller.translateSliderX.valueProperty().setAndBind(it.deltaXProperty, true)
                    controller.translateSliderY.valueProperty().setAndBind(it.deltaYProperty, true)
                    controller.translateSliderZ.valueProperty().setAndBind(it.deltaZProperty, true)
                }
                TransformationType.ROTATE -> {
                    controller.rotateSliderX.valueProperty().setAndBind(it.deltaXProperty, true)
                    controller.rotateSliderY.valueProperty().setAndBind(it.deltaYProperty, true)
                    controller.rotateSliderZ.valueProperty().setAndBind(it.deltaZProperty, true)
                }
                else -> {}
            }
        }
        gizmo
    }

    fun selectGizmo(){
        SubScene3D.mouseListener.set(EventHandler {
            if (gizmo.selectedAxis.get() != null) {
                val line3D = GizmoStackoverflow.getPickRay(SubScene3D.cameraHandler.camera, it)
                gizmo.controller.position =
                    Vector3f(gizmo.translateX.toFloat(), gizmo.translateY.toFloat(), gizmo.translateZ.toFloat())
                when(gizmo.transformMode) {
                    GizmoStackoverflow.TransformMode.TRANSLATE -> {
                        gizmo.controller.manipulateTranslateGizmo(line3D.toRay())
                    }
                    GizmoStackoverflow.TransformMode.ROTATE -> {
                        gizmo.controller.manipulateRotateGizmo(line3D.toRay())
                    }
                }
                it.consume()
            }
        })
        if (!getSelectionMesh().children.contains(gizmo))
            getSelectionMesh().children.add(gizmo)

    }

    fun unselectGizmo(){
        SubScene3D.mouseListener.set(null)
        getSelectionMesh().children.remove(gizmo)
    }

    fun getSelectionMesh(): Group {
        if (!this::selectionMesh.isInitialized) {
            selectionMesh = Group()
            var meshId = 0
            var totalX = 0.0
            var totalY = 0.0
            var totalZ = 0.0


            val controller = gizmo.controller
//            val t = Translate(
//                controller.translateSliderX.value,
//                controller.translateSliderY.value,
//                controller.translateSliderZ.value)
//
//            t.xProperty().setAndBind(controller.translateSliderX.valueProperty())
//            t.yProperty().setAndBind(controller.translateSliderY.valueProperty())
//            t.zProperty().setAndBind(controller.translateSliderZ.valueProperty())

            for (i in 0 until rootTransformation.groupIndices.size()) {
                val groupIndex = rootTransformation.groupIndices[i]
                for (model in entity.getModels()) {
                    val vertices = model.getVertexGroups().getOrNull(groupIndex) ?: continue
                    val sphere = getSelectionBox(meshId++, model, vertices)
                    totalX += sphere.translateX
                    totalY += sphere.translateY
                    totalZ += sphere.translateZ
//                    sphere.transforms.add(t)
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
//        model.getSceneNode().boundsInLocalProperty().onInvalidation {
//            computerCenter(selectionBox, model, vertices)
//        }
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
//        val box = Box()
//        box.width = width.toDouble()
//        box.height = height.toDouble()
//        box.depth = depth.toDouble()
        sphere.radius = 3.0
        sphere.translateX = (minX + width.toDouble().div(2))
        sphere.translateY = (minY + height.toDouble().div(2))
        sphere.translateZ = (minZ + depth.toDouble().div(2))
    }
}