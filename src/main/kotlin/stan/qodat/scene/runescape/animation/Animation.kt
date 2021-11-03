package stan.qodat.scene.runescape.animation

import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.embed.swing.SwingFXUtils
import javafx.scene.Node
import javafx.scene.SnapshotParameters
import javafx.scene.control.ContextMenu
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.util.Callback
import stan.gifencoder.*
import stan.qodat.Qodat
import stan.qodat.cache.Cache
import stan.qodat.cache.CacheEncoder
import stan.qodat.cache.definition.AnimationDefinition
import stan.qodat.javafx.menu
import stan.qodat.javafx.menuItem
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.control.LabeledHBox
import stan.qodat.scene.transform.Transformable
import stan.qodat.scene.transform.Transformer
import stan.qodat.util.Searchable
import stan.qodat.util.ViewNodeProvider
import stan.qodat.util.getAnimationsView
import java.io.File
import java.io.FileOutputStream
import java.io.UnsupportedEncodingException
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO


/**
 * Represents a [Transformer] for [Model] objects.
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class Animation(
    label: String,
    private val definition: AnimationDefinition,
    private val cache: Cache
) : Transformer<AnimationFrame>, Searchable, ViewNodeProvider, CacheEncoder {

    private lateinit var frames : ObservableList<AnimationFrame>
    private lateinit var skeletons : ObservableMap<Int, AnimationSkeleton>
    private lateinit var viewBox : HBox
    private var currentFrameIndex = 0

    val playingProperty = SubScene3D.animationPlayer.transformerProperty.isEqualTo(this)
    val labelProperty = SimpleStringProperty(label)

    fun getSkeletons() : ObservableMap<Int, AnimationSkeleton> {
        if (!this::skeletons.isInitialized){
            val skeletonsMap = definition.frameHashes
                .map { cache.getAnimationSkeletonDefinition(it) }
                .distinctBy { it.id }
                .map { it.id to AnimationSkeleton("${it.id}", it) }
                .toMap()
            skeletons = FXCollections.observableMap(skeletonsMap)
        }
        return skeletons
    }

    override fun getFrameList() : ObservableList<AnimationFrame> {
        if (!this::frames.isInitialized){
            val framesArray = Array(definition.frameHashes.size) { idx ->
                val frameDefinition = cache.getFrameDefinition(definition.frameHashes[idx])!!
                AnimationFrame(
                        name = "frame[$idx]",
                        definition = frameDefinition,
                        duration = definition.frameLengths[idx])
            }
            frames = FXCollections.observableArrayList(*framesArray)
        }
        return frames
    }

    override fun setFrame(frameIndex: Int) {
        currentFrameIndex = frameIndex
    }

    override fun update(transformableList: List<Transformable>) {

        val frames = getFrameList()
        val frame = frames[currentFrameIndex]

        if (!frame.enabledProperty.get())
            return

        transformableList.forEach {
            it.animate(frame)
        }
    }

    override fun getName() = labelProperty.get()

    override fun getViewNode(): Node {
        if (!this::viewBox.isInitialized) {
            viewBox = LabeledHBox(labelProperty).apply {
                label.contextMenu = ContextMenu().apply {
                    menu("export") {
                        menuItem("GIF") {
                            val animation = Qodat.getAnimationsView().selectionModel.selectedItem
                            if (animation != null) {
                                val scene = SubScene3D.subSceneProperty.get()
                                val animationPlayer = SubScene3D.animationPlayer
                                val out = FileOutputStream("${animation.getName()}.gif")
                                val options = ImageOptions()
                                val encoder = GifEncoder(out, scene.width.toInt(), scene.height.toInt(), 0)
                                for ((index, frame) in animation.frames.withIndex()) {
                                    animationPlayer.jumpToFrame(index)
                                    val snapshotParameters = SnapshotParameters().apply { fill = Color.BLACK }
                                    val snapShot =
                                        SubScene3D.subSceneProperty.get().snapshot(snapshotParameters, null)!!
                                    val reader = snapShot.pixelReader
                                    val colors = Array(snapShot.height.toInt()) { y ->
                                        Array(snapShot.width.toInt()) { x ->
                                            val color = reader.getColor(x, y)
                                            stan.gifencoder.Color(color.red, color.green, color.blue)
                                        }
                                    }
                                    val duration = frame.getDuration()
                                    val gifImage = Image.fromColors(colors)
                                    options.setDelay(duration.toMillis().toLong(), TimeUnit.MILLISECONDS)
                                    options.setTransparencyColor(stan.gifencoder.Color.BLACK.rgbInt)
                                    options.setDisposalMethod(DisposalMethod.DO_NOT_DISPOSE)
//                                    options.setColorQuantizer(KMeansQuantizer.INSTANCE)
                                    encoder.addImage(gifImage, options)
                                }
                                encoder.finishEncoding()
                                out.flush()
                                out.close()
                            }
                        }
                    }
                }
            }
        }
        return viewBox
    }

    override fun encode(format: Cache) : File {
        throw UnsupportedEncodingException()
    }

    companion object {
        fun createCellFactory() = Callback<ListView<Animation>, ListCell<Animation>> {
            object : ListCell<Animation>() {
                override fun updateItem(item: Animation?, empty: Boolean) {
                    super.updateItem(item, empty)
                    graphic = if (empty || item == null) null else item.getViewNode()
                }
            }
        }
    }
}