package stan.qodat.scene.controller

import javafx.beans.InvalidationListener
import javafx.beans.binding.Bindings
import javafx.beans.binding.DoubleBinding
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections
import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.Pos
import javafx.scene.Group
import javafx.scene.SnapshotParameters
import javafx.scene.control.*
import javafx.scene.input.ClipboardContent
import javafx.scene.input.TransferMode
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.scene.shape.Sphere
import javafx.stage.FileChooser
import javafx.util.Callback
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.control.DragRegion
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.animation.Transformation
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.scene.runescape.model.Model
import stan.qodat.task.EntityExportObjTask
import stan.qodat.task.ModelExportObjTask
import stan.qodat.util.*
import java.io.File
import java.net.URL
import java.util.*
import javax.imageio.ImageIO

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   29/01/2021
 */
class TimeLineController : Initializable {

    @FXML
    lateinit var timeLineBox: HBox
    @FXML
    lateinit var frameList: ListView<AnimationFrame>
    @FXML
    lateinit var transformsList: ListView<Transformation>

    private val transformSpheres = Group()

    override fun initialize(location: URL?, resources: ResourceBundle?) {

        HBox.setHgrow(frameList, Priority.ALWAYS)

        timeLineBox.children.removeAll(transformsList)

//        frameList.contextMenu = ContextMenu(
//            Menu("Export", null, MenuItem(".obj").apply {
//                disableProperty().setAndBind(Properties.selectedAnimation.isNull)
//                setOnAction {
//                    frameList.items.add(AnimationFrame("whoop", null, 5))
//                }
//            }),
//            MenuItem("Add frame").apply {
//                disableProperty().setAndBind(Properties.selectedAnimation.isNull)
//                setOnAction {
//                    frameList.items.add(AnimationFrame("whoop", null, 5))
//                }
//            }
//
//        )
        frameList.run {
            setOnDragDropped {
                it.consume()
            }
            setOnDragDetected {
                val animationPlayer = SubScene3D.contextProperty.get()?.animationPlayer
                val dragboard = startDragAndDrop(TransferMode.COPY)
                val clipboardContent = ClipboardContent()
                val files = selectionModel.selectedIndices.map { frameIndex ->
                    animationPlayer?.jumpToFrame(frameIndex)
                    val snapshotParameters = SnapshotParameters().apply { fill = Color.TRANSPARENT }
                    val snapShot = SubScene3D.subSceneProperty.get().snapshot(snapshotParameters, null)
                    val image = SwingFXUtils.fromFXImage(snapShot, null)
                    val name = Properties.selectedAnimationName.get() + "_" + frameIndex.toString()
                    val out = Properties.exportsPath.get().resolve("png").resolve("$name.png").toFile().apply {
                        if (!parentFile.exists())
                            parentFile.mkdir()
                        if (!exists())
                            createNewFile()
                    }
                    ImageIO.write(image, "png", out)
                    out
                }
                clipboardContent.putFiles(files)
                dragboard.setContent(clipboardContent)
                it.consume()
            }
            setOnDragExited {
                it.consume()
            }
            setOnDragDone {
                it.consume()
            }
        }
        frameList.selectionModel.selectionMode = SelectionMode.MULTIPLE
        frameList.onIndexSelected {
            if (this >= 0) {
                val animationPlayer = SubScene3D.contextProperty.get()?.animationPlayer
                if (animationPlayer != null) {
                    if (this != animationPlayer.frameIndexProperty.get()) {
                        animationPlayer.jumpToFrame(this)
                        val frame = frameList.items[this]
                        val groupIndicesList = frame.transformationList.map {
                            it.groupIndices.toArray(null)
                        }
                        transformSpheres.children.clear()

                        animationPlayer.transformableList.forEach {

                            if (it is Model) {
                                var previousSphere: Sphere? = null
                                for (groupIndices in groupIndicesList) {
                                    for (group in groupIndices) {
                                        val vertices = it.getVertexGroup(group)
                                        if (vertices.isEmpty())
                                            continue
                                        var totalX = 0
                                        var totalY = 0
                                        var totalZ = 0
                                        for (vertex in vertices) {
                                            totalX += it.getX(vertex)
                                            totalY += it.getY(vertex)
                                            totalZ += it.getZ(vertex)
                                        }
                                        val centerX = totalX / vertices.size
                                        val centerY = totalY / vertices.size
                                        val centerZ = totalZ / vertices.size
                                        val sphere = Sphere(3.0)
                                        if (previousSphere != null) {
                                            //TODO: 3d lines between spheres
                                        }
                                        previousSphere = sphere

                                        sphere.translateX = centerX.toDouble()
                                        sphere.translateY = centerY.toDouble()
                                        sphere.translateZ = centerZ.toDouble()
                                        transformSpheres.children.add(sphere)
                                    }
                                }
                            }
                        }
//                    SubScene3D.contentGroupProperty.get().children.add(transformSpheres)
                    }

//                if (!timeLineBox.children.contains(transformsList))
//                    timeLineBox.children.add(transformsList)
//
//                transformsList.selectionModel.clearSelection()
//                transformsList.cellFactory = Callback {
//                    createTransformationCell()
//                }
//                transformsList.items.setAll(frameList.items[this].transformationList)
                }
            }
//            else timeLineBox.children.remove(transformsList)
        }

//        animationPlayer.frameIndexProperty.addListener { _, _, newValue ->
//            val content = SubScene3D.contentGroupProperty.get()
//            if (content.children.contains(transformSpheres))
//                content.children.remove(transformSpheres)
//            if (newValue.toInt() >= 0 && newValue != frameList.selectionModel.selectedIndex){
//                frameList.selectionModel.select(newValue.toInt())
//            }
//        }

        val totalDurationProperty = SimpleIntegerProperty(0)

        frameList.cellFactory = Callback {
            createFrameCell(totalDurationProperty, Qodat.mainController.bottomBox.widthProperty().subtract(1))
        }

        val durationChangedListener = InvalidationListener {
            updateTotalDuration(totalDurationProperty)
        }

        frameList.itemsProperty().addListener(durationChangedListener)
        frameList.itemsProperty().addListener { _, oldValue, newValue ->
            oldValue?.forEach {
                it.durationProperty.removeListener(durationChangedListener)
            }
            oldValue?.removeListener(durationChangedListener)
            newValue?.forEach {
                it.durationProperty.addListener(durationChangedListener)
            }
            newValue?.addListener(durationChangedListener)
        }

        Properties.selectedAnimation.addListener { _, _, animation ->

            frameList.selectionModel.clearSelection()

            if (animation == null) {
                frameList.items = FXCollections.emptyObservableList()
                return@addListener
            }

            if (timeLineBox.children.contains(transformsList))
                timeLineBox.children.remove(transformsList)

            frameList.items = animation.getFrameList()
        }
    }

    private fun updateTotalDuration(totalDurationProperty: SimpleIntegerProperty) {
        var totalDuration = 0

        for (animationFrame in frameList.items)
            totalDuration += FrameTimeUtil.toFrameAsInt(animationFrame.getDuration())

        totalDurationProperty.set(totalDuration)
    }


    private fun createFrameCell(
        totalDuration: SimpleIntegerProperty,
        maxWidthProperty: DoubleBinding
    ): ListCell<AnimationFrame> {
        val cell = ListCell<AnimationFrame>()
        cell.alignment = Pos.CENTER

        cell.itemProperty().addListener { _, _, newValue ->
            if (newValue != null) {
                val frame = cell.item

                val frameLength = Bindings
                    .createIntegerBinding({ FrameTimeUtil.toFrameAsInt(frame.getDuration()) },
                    frame.durationProperty)

                val cellIndex = cell.index
                val first = cellIndex == 0
                cell.style =
                    "    -fx-background-insets: 0 1 0 0;\n" +
//                                "    -fx-background-radius: 0;\n" +
                            "    -fx-text-fill: #828282;\n" +
                            "    -fx-font-size: 12px;\n" +
                            "    -fx-font-weight: bold;\n" +
                            "    -fx-border-color: rgb(174, 138, 190);\n" +
                            "    -fx-border-width: 0 1 0 0"
                val transformationTypeDetails = frame.transformationList
                    .groupBy { it.getType() }
                    .mapValues { it.value.size }
                    .entries
                    .map { "${it.value}x ${it.key} transforms" }
                    .joinToString("\n")
                cell.textFill = Color.CYAN
                cell.text = cellIndex.toString()
                cell.tooltip = Tooltip(transformationTypeDetails)
                cell.contextMenu = ContextMenu(
                    Menu("Export", null, MenuItem(".obj/.mtl").apply {
                        setOnAction {
                            val entity = Properties.selectedEntity.get()

                            val fileChooser = FileChooser()
                            fileChooser.title = "Export to WaveFront format."
                            fileChooser.initialFileName = formatExportName(entity, cellIndex)
                            val file = fileChooser.showSaveDialog(null)
                            if (file != null) {

                                val model = entity.createMergedModel(file.nameWithoutExtension)
                                Qodat.mainController.executeBackgroundTasks(ModelExportObjTask(file, model, cell.item))
                            }
                        }
                    })
                )

               cell.prefWidthProperty().bind(Bindings.createDoubleBinding({
                    frameLength.get().times(maxWidthProperty.get()).div(totalDuration.get())
                }, totalDuration, maxWidthProperty))
//                binding.addListener { _, _, newWidth ->
//                    setPrefwidth(pixelToDurationRatio, frameLength, newWidth, cell)
//                }
//                setPrefwidth(pixelToDurationRatio, frameLength, binding.get(), cell)
//
                DragRegion(
                    cell.widthProperty(), cell.heightProperty(), cell,
                    relativeBounds = null,
//                    DragRegion.RelativeBounds(
//                        DragRegion.Placement.TOP_RIGHT,
//                        widthProperty = Bindings.subtract(cell.widthProperty(), 10.0),
//                        heightProperty = cell.heightProperty()
//                    )
                ) { deltaX ->
//                    val min = frameLength.get().div(cell.width)
//                    if (deltaX.absoluteValue > 0.005) {
//                        println(deltaX)
//                        val newWidth = cell.width + deltaX
////                    cell.prefWidth(newWidth)
//                        val newLength = (frameLength.get() + (-1 * deltaX.sign.toInt()))
//                            .coerceAtLeast(1)
//                            .coerceAtMost(255)
//                        if (newLength != frameLength.get()) {
//                            println("${frame.idProperty.get()}: $frameLength -> $newLength")
//
//                            frame.durationProperty.set(FrameTimeUtil.frame(newLength))
//                            updateTotalDuration(totalDuration)
//                        }
//                    }
                }
            }
        }
        return cell
    }

    private fun formatExportName(
        entity: Entity<*>,
        cellIndex: Int
    ) = entity.getName() + "_" + Properties.selectedAnimationName.get() + "_$cellIndex"

    private fun setPrefwidth(
        pixelToDurationRatio: SimpleDoubleProperty,
        frameLength: Int,
        newWidth: Number,
        cell: ListCell<AnimationFrame>
    ) {
        pixelToDurationRatio.value = frameLength.div(newWidth.toDouble())
        cell.prefWidth(newWidth.toDouble())
        println(newWidth)
    }

    private fun createTransformationCell(): ListCell<Transformation> {
        val cell = ListCell<Transformation>()
        cell.alignment = Pos.CENTER

        cell.itemProperty().addListener { _, _, newValue ->
            if (newValue != null) {
                val transformation = cell.item
                val dx = transformation.getDeltaX()
                val dy = transformation.getDeltaY()
                val dz = transformation.getDeltaZ()
                val type = transformation.getType()
                val vertexGroupIndices = transformation.groupIndices.toArray(null)

                cell.textFill = Color.web("#AE8ABE")
                cell.text = "${type.name.toLowerCase().replace("_", " ")}\t($dx, $dy, $dz)"
                cell.tooltip = Tooltip(vertexGroupIndices.joinToString { "," })
            }
        }
        return cell
    }
}
