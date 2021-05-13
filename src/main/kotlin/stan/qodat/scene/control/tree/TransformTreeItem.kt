package stan.qodat.scene.control.tree

import javafx.collections.FXCollections
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.DrawMode
import stan.qodat.scene.control.TreeItemListContextMenu
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.animation.Transformation
import stan.qodat.scene.runescape.animation.TransformationType
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.scene.runescape.model.ModelFaceMesh
import stan.qodat.util.BABY_BLUE
import stan.qodat.util.setAndBind

class TransformTreeItem(
    private val entity: Entity<*>,
    private val frame: AnimationFrame,
    private val transform: Transformation,
    transformsItem: TreeItem<Node>,
    selectionModel: MultipleSelectionModel<TreeItem<Node>>
) : TreeItem<Node>() {

    private lateinit var selectionMesh: Group

    init {

        val controlBox = HBox(5.0)
        controlBox.alignment = Pos.CENTER_LEFT

        val disableBox = CheckBox()
        disableBox.selectedProperty().setAndBind(transform.enabledProperty, biDirectional = true)
        controlBox.children.add(disableBox)

        val label = Label()
        label.textProperty().setAndBind(transform.labelProperty)

        val contextMenu = AnimationTreeItem.transformsContextMenuMap.getOrPut(frame) {
            TreeItemListContextMenu(
                list = frame.transformationList,
                rootItem = transformsItem,
                selectionModel = selectionModel,
                transformer = { type, transform ->
                    if (type == TreeItemListContextMenu.CreateActionType.DUPLICATE)
                        transform.clone()
                    else
                        Transformation("New")
                }
            )
        }
        label.contextMenu = contextMenu
        controlBox.children.add(label)

        val controlBox2 = VBox(5.0)
        controlBox2.alignment = Pos.CENTER_LEFT
        val transformTypeList = FXCollections.observableArrayList(*TransformationType.values())
        val typeBox = ComboBox(transformTypeList)
        typeBox.selectionModel.select(transform.typeProperty.get())
        transform.typeProperty.bind(typeBox.selectionModel.selectedItemProperty())
        controlBox2.children.add(typeBox)

        val transformControlItem = TreeItem<Node>(controlBox2)
        transformControlItem.isExpanded = true
        children.add(transformControlItem)

        valueProperty().set(controlBox)

        selectionModel.selectedItemProperty().addListener { _, oldValue, newValue ->
            if (newValue == this) {
                if (oldValue !is TransformTreeItem) {
                    entity.getModels().forEach {
                        it.drawModeProperty.set(DrawMode.LINE)
                    }
                }
                entity.getSceneNode().children.addAll(
                    getSelectionMesh()
                )
            } else if (oldValue == this) {
                if (newValue !is TransformTreeItem) {
                    entity.getModels().forEach {
                        it.drawModeProperty.set(DrawMode.FILL)
                    }
                }
                entity.getSceneNode().children.removeAll(
                    getSelectionMesh()
                )
            }
        }
    }

    private fun getSelectionMesh(): Group {
        if (!this::selectionMesh.isInitialized) {
            val selectionMaterial = PhongMaterial(BABY_BLUE)
            selectionMesh = Group()
            for (i in 0 until transform.groupIndices.size()) {
                val groupIndex = transform.groupIndices[i]
                for (model in entity.getModels()) {
                    val faceIndices = model.getFaceGroups().getOrNull(groupIndex) ?: continue
                    for (face in faceIndices) {
                        val mesh = ModelFaceMesh(face, selectionMaterial)
                        val (v1, v2, v3) = model.getVertices(face)
                        val vertexIndex1 = mesh.addVertex(v1, model.getX(v1), model.getY(v1), model.getZ(v1))
                        val vertexIndex2 = mesh.addVertex(v2, model.getX(v2), model.getY(v2), model.getZ(v2))
                        val vertexIndex3 = mesh.addVertex(v3, model.getX(v3), model.getY(v3), model.getZ(v3))
                        val u = floatArrayOf(-1f, -1f, -1f)
                        val v = floatArrayOf(-1f, -1f, -1f)
                        val texIndex1 = mesh.addUV(u[0], v[0])
                        val texIndex2 = mesh.addUV(u[1], v[1])
                        val texIndex3 = mesh.addUV(u[2], v[2])
                        mesh.faces.addAll(
                            vertexIndex1, texIndex1,
                            vertexIndex2, texIndex2,
                            vertexIndex3, texIndex3
                        )
                        mesh.drawModeProperty.set(DrawMode.FILL)
                        mesh.visibleProperty.set(true)
                        selectionMesh.children.add(mesh.getSceneNode())
                    }
                }
            }
        }
        return selectionMesh
    }
}