package stan.qodat.scene.controller

import javafx.beans.InvalidationListener
import javafx.beans.binding.Bindings
import javafx.beans.binding.DoubleBinding
import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections
import javafx.embed.swing.SwingFXUtils
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.geometry.Pos
import javafx.scene.SnapshotParameters
import javafx.scene.control.*
import javafx.scene.input.ClipboardContent
import javafx.scene.input.TransferMode
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.util.Callback
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.control.export.ExportMenu
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.animation.AnimationFrameLegacy
import stan.qodat.scene.runescape.animation.Transformation
import stan.qodat.scene.runescape.entity.Entity
import stan.qodat.util.FrameTimeUtil
import stan.qodat.util.onIndexSelected
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

    override fun initialize(location: URL?, resources: ResourceBundle?) {

        HBox.setHgrow(frameList, Priority.ALWAYS)

        timeLineBox.children.removeAll(transformsList)

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
                    val out = Properties.defaultExportsPath.get().resolve("png").resolve("$name.png").toFile().apply {
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
                    }
                }
            }
        }

        val totalDurationProperty = SimpleIntegerProperty(0)

        frameList.cellFactory = Callback {
            createFrameCell(totalDurationProperty, Qodat.mainController.bottomBox.widthProperty().subtract(1))
        }

        val durationChangedListener = InvalidationListener {
            updateTotalDuration(totalDurationProperty)
        }

        frameList.contextMenu = ContextMenu(
            ExportMenu<Entity<*>>().apply {
                bindExportable(Properties.selectedEntity)
                bindAnimation(Properties.selectedAnimation)
                bindFrameList(frameList)
            }
        )

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
                    .createIntegerBinding(
                        { FrameTimeUtil.toFrameAsInt(frame.getDuration()) },
                        frame.durationProperty
                    )

                val cellIndex = cell.index
                cell.style =
                    "    -fx-background-insets: 0 1 0 0;\n" +
                            "    -fx-text-fill: #828282;\n" +
                            "    -fx-font-size: 12px;\n" +
                            "    -fx-font-weight: bold;\n" +
                            "    -fx-border-color: rgb(174, 138, 190);\n" +
                            "    -fx-border-width: 0 1 0 0"
                if (frame is AnimationFrameLegacy) {
                    val transformationTypeDetails = frame.transformationList
                        .groupBy { it.getType() }
                        .mapValues { it.value.size }
                        .entries
                        .map { "${it.value}x ${it.key} transforms" }
                        .joinToString("\n")
                    cell.tooltip = Tooltip(transformationTypeDetails)
                }
                cell.textFill = Color.CYAN
                cell.text = cellIndex.toString()
                cell.prefWidthProperty().bind(Bindings.createDoubleBinding({
                    frameLength.get().times(maxWidthProperty.get()).div(totalDuration.get())
                }, totalDuration, maxWidthProperty))
            }
        }
        return cell
    }
}
