package stan.qodat.scene.runescape.animation

import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.scene.Node
import javafx.scene.layout.HBox
import stan.qodat.cache.definition.AnimationDefinition
import stan.qodat.cache.Cache
import stan.qodat.cache.CacheEncoder
import stan.qodat.scene.SubScene3D
import stan.qodat.util.ViewNodeProvider
import stan.qodat.scene.control.LabeledHBox
import stan.qodat.scene.transform.Transformable
import stan.qodat.scene.transform.Transformer
import stan.qodat.util.Searchable
import java.io.UnsupportedEncodingException

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
        if (!this::viewBox.isInitialized)
            viewBox = LabeledHBox(labelProperty)
        return viewBox
    }

    override fun encode(format: Cache) {
        throw UnsupportedEncodingException()
    }
}