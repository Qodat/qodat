package stan.qodat.scene.control.tree

import IntField
import javafx.beans.InvalidationListener
import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.Box
import javafx.scene.shape.CullFace
import javafx.scene.shape.DrawMode
import javafx.scene.shape.Sphere
import stan.qodat.javafx.*
import stan.qodat.scene.control.TreeItemListContextMenu
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.animation.Transformation
import stan.qodat.scene.runescape.animation.TransformationType
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.scene.runescape.model.Model
import stan.qodat.util.BABY_BLUE
import stan.qodat.util.onInvalidation
import kotlin.math.abs

open class TransformTreeItem(
    private val entity: Entity<*>,
    private val frame: AnimationFrame,
    private val transform: Transformation,
    transformsItem: TreeItem<Node>,
    selectionModel: MultipleSelectionModel<TreeItem<Node>>
) : TreeItem<Node>() {

    private lateinit var selectionMesh: Group

    init {
        hBox(isGraphic = true) {
            checkBox(transform.enabledProperty, biDirectional = true)
            label(transform.labelProperty) {
                contextMenu = AnimationTreeItem.transformsContextMenuMap.getOrPut(frame) {
                    TreeItemListContextMenu(
                        list = frame.transformationList,
                        rootItem = transformsItem,
                        selectionModel = selectionModel,
                        itemCreator = { type, transform ->
                            if (type == TreeItemListContextMenu.CreateActionType.DUPLICATE)
                                transform.clone()
                            else
                                Transformation("New")
                        }
                    )
                }
            }
            vBox {
                comboBox("type", TransformationType.values(), transform.typeProperty)
                addEditControls()
                addGroupList()
            }
        }

        selectionModel.onSelected { oldValue, newValue ->
            if (newValue == this) {
                if (oldValue !is TransformTreeItem && oldValue !is TransformGroupTreeItem)
                    for (model in entity.getModels())
                        model.drawModeProperty.set(DrawMode.LINE)
                entity.getSceneNode().children.addAll(getSelectionMesh())
            } else if (oldValue == this) {
                if (newValue !is TransformTreeItem && oldValue !is TransformGroupTreeItem)
                    for (model in entity.getModels())
                        model.drawModeProperty.set(DrawMode.FILL)
                entity.getSceneNode().children.removeAll(getSelectionMesh())
            }
        }
    }

    private fun VBox.addGroupList() {
        val listView = ListView<Int>()
        transform.groupIndices.onInvalidation {
            val ints = transform.groupIndices.toArray(null).toList()
            listView.items.setAll(ints)
        }
        val ints = transform.groupIndices.toArray(null).toList()
        listView.items.setAll(ints)
        listView.prefHeightProperty().bind(Bindings.size(transform.groupIndices).multiply(28));
        children.add(listView)
    }

    private fun VBox.addEditControls() {
        val initialX = transform.getDeltaX()
        val initialY = transform.getDeltaY()
        val initialZ = transform.getDeltaZ()
        val sliderX = Slider(-255.0, 255.0, transform.getDeltaX().toDouble())
        val sliderY = Slider(-255.0, 255.0, transform.getDeltaY().toDouble())
        val sliderZ = Slider(-255.0, 255.0, transform.getDeltaZ().toDouble())
        sliderX.valueProperty().bindBidirectional(transform.deltaXProperty)
        sliderY.valueProperty().bindBidirectional(transform.deltaYProperty)
        sliderZ.valueProperty().bindBidirectional(transform.deltaZProperty)
        val resetX = Button("Undo")
        val resetY = Button("Undo")
        val resetZ = Button("Undo")
        val textFieldX = IntField(-255, 255, transform.getDeltaX())
        val textFieldY = IntField(-255, 255, transform.getDeltaY())
        val textFieldZ = IntField(-255, 255, transform.getDeltaZ())
        textFieldX.valueProperty().bindBidirectional(transform.deltaXProperty)
        textFieldY.valueProperty().bindBidirectional(transform.deltaYProperty)
        textFieldZ.valueProperty().bindBidirectional(transform.deltaZProperty)
        val boxX = HBox(textFieldX, sliderX, resetX)
        val boxY = HBox(textFieldY, sliderY, resetY)
        val boxZ = HBox(textFieldZ, sliderZ, resetZ)
        resetX.setOnAction { transform.deltaXProperty.set(initialX) }
        resetY.setOnAction { transform.deltaYProperty.set(initialY) }
        resetZ.setOnAction { transform.deltaZProperty.set(initialZ) }
        Bindings.createBooleanBinding(
            {
                true
            }, transform.deltaXProperty, transform.deltaYProperty, transform.deltaZProperty
        )
        val invalidationListener = InvalidationListener {
            entity.getModels().forEach {
                it.animate(frame)
            }
        }
        transform.deltaXProperty.addListener(invalidationListener)
        transform.deltaYProperty.addListener(invalidationListener)
        transform.deltaZProperty.addListener(invalidationListener)
        children.addAll(boxX, boxY, boxZ)
    }

    private fun getSelectionMesh(): Group {
        if (!this::selectionMesh.isInitialized) {
            val selectionMaterial = PhongMaterial(BABY_BLUE)
            selectionMesh = Group()
            var meshId = 0
            for (i in 0 until transform.groupIndices.size()) {
                val groupIndex = transform.groupIndices[i]
                for (model in entity.getModels()) {
                    val vertices = model.getVertexGroups().getOrNull(groupIndex) ?: continue
                    selectionMesh.children.add(getSelectionBox(meshId++, model, vertices))
                }
            }
        }
        return selectionMesh
    }

    private val selectionBoxes = mutableMapOf<Int, Box>()

    private fun getSelectionBox(meshId: Int, model: Model, vertices: IntArray) : Sphere {
        val selectionBox = Sphere()
        selectionBox.id = "$meshId"
        selectionBox.cullFace = CullFace.FRONT
        selectionBox.drawMode = DrawMode.LINE
        selectionBox.material = PhongMaterial(Color.web("#CC7832"))
        model.getSceneNode().boundsInLocalProperty().onInvalidation {
            computerCenter(selectionBox, model, vertices)
        }
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
        val box = Box()
//        box.width = width.toDouble()
//        box.height = height.toDouble()
//        box.depth = depth.toDouble()
        sphere.radius = 3.0
        sphere.translateX = (minX + width.toDouble().div(2))
        sphere.translateY = (minY + height.toDouble().div(2))
        sphere.translateZ = (minZ + depth.toDouble().div(2))
    }
}