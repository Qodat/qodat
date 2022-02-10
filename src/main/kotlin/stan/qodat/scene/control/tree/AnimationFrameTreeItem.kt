package stan.qodat.scene.control.tree

import IntField
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.text.*
import stan.qodat.Properties
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.javafx.checkBox
import stan.qodat.javafx.hBox
import stan.qodat.javafx.onSelected
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.runescape.animation.Animation
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.animation.Transformation
import stan.qodat.scene.runescape.animation.TransformationType
import stan.qodat.scene.runescape.entity.AnimatedEntity
import stan.qodat.util.*
import java.util.concurrent.Callable
import javax.swing.Box

class AnimationFrameTreeItem(
    val entity: AnimatedEntity<*>,
    val animation: Animation,
    val frame: AnimationFrame,
    val treeView: TreeView<Node>
) : TreeItem<Node>(), LineMode {

    private val selectedProperty = SimpleBooleanProperty()

    init {

        hBox(spacing = 15.0) {
//            checkBox(frame.enabledProperty, biDirectional = true)

            val frameContextMenu = ContextMenu(
                createInterpolationMenuItem(),
                createDuplicateMenuItem(),
                createRemoveMenuItem()
            )

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

                children += Label(" Length: ")
                val frameDurationField = IntField(0, 255, FrameTimeUtil.toFrameAsInt(frame.durationProperty.get()))
                children += frameDurationField
                frameDurationField.maxWidthProperty().set(35.0)
                frameDurationField.valueProperty().addListener { _, _, newValue ->
                    frame.durationProperty.set(FrameTimeUtil.frame(newValue.toInt()))
                }
            }
        }

        treeView.selectionModel.onSelected { oldValue, newValue ->
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
        val fileId = OldschoolCacheRuneLite.getFileId(hexString)
        val frameId = OldschoolCacheRuneLite.getFrameId(hexString)
        return fileId to frameId
    }

    fun resetTransformTreeItems(
        list: ObservableList<Transformation>,
        entity: AnimatedEntity<*>,
        frame: AnimationFrame,
        treeView: TreeView<Node>
    ) {
        children.clear()

        val flat = false
        if (!flat) {
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
        } else {
            for ((index, transform) in list.withIndex())
                this.children.add(
                    index,
                    TransformTreeItem(entity, frame, transform, this, treeView.selectionModel)
                )
        }
    }

    private fun shiftFrameIndices(index: Int) {
        val frameIterator = animation.getFrameList().subList(index + 1, animation.getFrameList().size).iterator()
        val frameId = frame.getFrameId(Integer.toHexString(frame.idProperty.get())) + 1
        var nextFrameId = frameId + 1
        var i = index + 1
        while (frameIterator.hasNext()) {
            val next = frameIterator.next()
            val hex = Integer.toHexString(next.idProperty.get())
            val fileId = next.getFileId(hex)
            next.idProperty.set(((fileId and 0xFFFF) shl 16) or (nextFrameId and 0xFFFF))
            next.labelProperty.set("frame[${++i}]")
            nextFrameId++
        }
    }

    private fun createRemoveMenuItem() =
        MenuItem("Remove").apply {
            setOnAction {

                val index = animation.getFrameList().indexOf(frame)

                animation.getFrameList().remove(frame)

                val frameIterator =
                    animation.getFrameList().subList(index, animation.getFrameList().size).iterator()
                val frameId = frame.getFrameId(Integer.toHexString(frame.idProperty.get()))
                var nextFrameId = frameId
                var i = index
                while (frameIterator.hasNext()) {
                    val next = frameIterator.next()
                    val hex = Integer.toHexString(next.idProperty.get())
                    val fileId = next.getFileId(hex)
                    next.idProperty.set(((fileId and 0xFFFF) shl 16) or (nextFrameId and 0xFFFF))
                    next.labelProperty.set("frame[${i++}]")
                    nextFrameId++
                }

            }
        }

    private fun createDuplicateMenuItem() =
        MenuItem("Duplicate").apply {
            setOnAction {
                val index = animation.getFrameList().indexOf(frame)
                shiftFrameIndices(index)
                animation.getFrameList().add(index + 1, frame.clone("frame[${index + 1}]"))
            }
        }

    private fun createInterpolationMenuItem() =
        MenuItem("Interpolate").apply {
            setOnAction {
                val frames = animation.getFrameList()
                var index = frames.indexOf(frame)
                var nextFrameIndex = index
                if (index + 1 < frames.size) {
                    nextFrameIndex = index + 1
                } else
                    nextFrameIndex = 0

                if (nextFrameIndex != index) {
                    val nextFrame = frames[nextFrameIndex]
                    if (frame.transformationCountProperty.get() == nextFrame.transformationCountProperty.get()) {
                        val transforms = mutableListOf<Transformation>()
                        for (i in 0 until frame.transformationCountProperty.get()) {
                            val initialTransform = frame.transformationList.get(i)
                            val transform = nextFrame.transformationList.get(i)
                            val dx = -(transform.getDeltaX() - initialTransform.getDeltaX()).div(2)
                            val dy = -(transform.getDeltaY() - initialTransform.getDeltaY()).div(2)
                            val dz = -(transform.getDeltaZ() - initialTransform.getDeltaZ()).div(2)
                            transforms.add(Transformation(
                                "transform[$i]",
                                transform.groupIndices.toArray(null),
                                transform.getType().ordinal,
                                transform.getDeltaX() + dx,
                                transform.getDeltaY() + dy,
                                transform.getDeltaZ() + dz
                            ).apply {
                                idProperty.set(i)
                                groupIndexProperty.set(transform.groupIndexProperty.get())
                            })
                        }
                        val interpolatedFrame = frame.clone("frame[${index + 1}]")
                        interpolatedFrame.transformationList.setAll(transforms)
                        shiftFrameIndices(index)
                        animation.getFrameList().add(index + 1, interpolatedFrame)
                    }
                }
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