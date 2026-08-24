package stan.qodat.scene.control.tree

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.text.Font
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import javafx.scene.text.Text
import stan.qodat.Properties
import stan.qodat.cache.impl.displee.DispleeCache
import stan.qodat.javafx.hBox
import stan.qodat.javafx.onTreeSelected
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.control.IntTextField
import stan.qodat.scene.runescape.animation.*
import stan.qodat.scene.runescape.entity.AnimatedEntity
import stan.qodat.util.FrameTimeUtil
import stan.qodat.util.onInvalidation
import stan.qodat.util.setAndBind

class AnimationFrameTreeItem(
    val entity: AnimatedEntity<*>,
    val animation: Animation,
    val frame: AnimationFrame,
    val treeView: TreeView<Node>
) : TreeItem<Node>(), LineMode {

    private val selectedProperty = SimpleBooleanProperty()

    init {

        hBox(spacing = 15.0) {
            children += HBox().apply {
                minHeight(100.0)
                isFillHeight = true
                spacing = 10.0
                alignment = Pos.CENTER_LEFT
                children += Text("FRAME").apply {
                    font = Font.font("Menlo", FontWeight.LIGHT, 13.0)
                    fill = Properties.treeItemAnimationFrameColor.get()
                    selectedProperty.onInvalidation {
                        font = if (get()) {
                            fill = Properties.treeItemAnimationFrameSelectedColor.get()
                            Font.font("Menlo",  FontWeight.EXTRA_BOLD, 13.0)
                        } else {
                            fill = Properties.treeItemAnimationFrameColor.get()
                            Font.font("Menlo", FontWeight.LIGHT, 13.0)
                        }
                    }
                }

                if (frame is AnimationFrameLegacy) {
                    if (frame.definition != null) {
                        val fileIdText = Text().apply {
                            font = Font.font("Menlo", 11.0)
                            fill = Color.web("#A4B8C8")
                        }
                        val frameIdText = Text().apply {
                            font = Font.font("Menlo", FontWeight.EXTRA_BOLD, 11.0)
                            fill = Color.web("#A4B8C8")
                        }
                        updateFileAndFrameIdTexts(frame.idProperty.get(), fileIdText, frameIdText)
                        frame.idProperty.onInvalidation {
                            val hash = get()
                            updateFileAndFrameIdTexts(hash, fileIdText, frameIdText)
                        }
                        children += fileIdText
                        children += frameIdText
                        children += Text().apply {
                            fill = Color.web("#A4B8C8")
                            font = Font.font("Menlo", FontWeight.EXTRA_LIGHT, FontPosture.ITALIC, 11.0)
                            textProperty().setAndBind(frame.idProperty.asString())
                        }
                    }
                }

                children += Label(" Length: ")
                val frameDurationField = IntTextField(0, Short.MAX_VALUE.toInt(), FrameTimeUtil.toFrameAsInt(frame.durationProperty.get()))
                children += frameDurationField
                frameDurationField.maxWidthProperty().set(35.0)
                frameDurationField.valueProperty().addListener { _, _, newValue ->
                    frame.durationProperty.set(FrameTimeUtil.frame(newValue.toInt()))
                }
            }
        }

        onTreeSelected(treeView.selectionModel) { oldValue, newValue ->
            if (newValue == this) {
                entity.animate(frame)
                entity.getSceneNode().children.addAll(getSelectionMesh())
                selectedProperty.set(true)
            } else if (oldValue == this) {
                entity.getSceneNode().children.removeAll(getSelectionMesh())
                SubScene3D.mouseListener.set(null)
                selectedProperty.set(false)
            }
        }
    }

    private fun updateFileAndFrameIdTexts(
        hash: Int,
        fileIdText: Text,
        frameIdText: Text
    ) {
        val (fileId, frameId) = updateFileAndFrameIdLabel(hash)
        fileIdText.textProperty().set(fileId.toString())
        frameIdText.textProperty().set(frameId.toString())
    }

    private fun updateFileAndFrameIdLabel(hash: Int): Pair<Int, Int> {
        val hexString = Integer.toHexString(hash)
        val fileId = DispleeCache.getFileId(hexString)
        val frameId = DispleeCache.getFrameId(hexString)
        return fileId to frameId
    }

    fun resetTransformTreeItems(
        list: ObservableList<Transformation>,
        entity: AnimatedEntity<*>,
        frame: AnimationFrame,
        treeView: TreeView<Node>
    ) {
        children.clear()

        val groupedTransformations = mutableMapOf<Transformation, List<Transformation>>()
        var children: MutableList<Transformation>? = null
        val transformationsIterator = list.iterator()
        while (transformationsIterator.hasNext()) {
            val next = transformationsIterator.next()
            next.bind(frame, entity)
            if (next.getType() == TransformationType.SET_OFFSET) {
                children = mutableListOf()
                groupedTransformations[next] = children
            } else {
                requireNotNull(children) { "First transform should be of type ${TransformationType.SET_OFFSET}" }
                    .add(next)
            }
        }
        for ((rootTransformation, childTransformations) in groupedTransformations) {
            this.children.add(
                TransformGroupTreeItem(
                    entity, frame, this, treeView,
                    rootTransformation, childTransformations
                )
            )
        }
    }

    private lateinit var selectionMesh: Group

    private val selectedMesh = SimpleObjectProperty<TransformGroupTreeItem>().apply {
        this.addListener { _, oldValue, newValue ->
            oldValue?.unselectGizmo()
            newValue?.selectGizmo()
        }
    }

    private fun getSelectionMesh(): Group {
        if (!this::selectionMesh.isInitialized) {
            selectionMesh = Group()

            for (child in children) {
                if (child is TransformGroupTreeItem) {
                    val mesh = child.getSelectionMesh()
                    selectionMesh.children += mesh
                    mesh.children.forEach {
                        it.setOnMousePressed { mouseEvent ->
                            selectedMesh.set(child)
                            mouseEvent.consume()
                        }
                    }
                }
            }
        }
        return selectionMesh
    }

}
