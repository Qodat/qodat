package stan.qodat.scene.runescape.animation

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.util.Callback
import qodat.cache.Cache
import qodat.cache.Encoder
import qodat.cache.definition.AnimationDefinition
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.javafx.menu
import stan.qodat.javafx.menuItem
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.control.LabeledHBox
import stan.qodat.scene.control.export.gif.AnimationToGifTask
import stan.qodat.scene.control.export.mp4.AnimationToMp4Task
import stan.qodat.scene.control.tree.AnimationTreeItem
import stan.qodat.scene.provider.ViewNodeProvider
import stan.qodat.scene.transform.Transformable
import stan.qodat.scene.transform.Transformer
import stan.qodat.task.BackgroundTasks
import stan.qodat.util.Searchable
import tornadofx.contextmenu
import tornadofx.item
import tornadofx.stringBinding

/**
 * Represents a [Transformer] for [Model] objects.
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class Animation(
    private val label: String,
    private val definition: AnimationDefinition? = null,
    private val cache: Cache? = null
) : Transformer<AnimationFrame>, Searchable, ViewNodeProvider, Encoder {

    private lateinit var frames: ObservableList<AnimationFrame>
    private lateinit var skeletons: ObservableMap<Int, AnimationSkeleton>
    private lateinit var viewBox: HBox

    private var currentFrameIndex = 0

    val treeItemProperty = SimpleObjectProperty<AnimationTreeItem>()
    val exportFrameArchiveId = SimpleIntegerProperty()
    val labelProperty = SimpleStringProperty(label)
    val nameProperty = SimpleStringProperty(label)
    val idProperty = SimpleIntegerProperty()
    val loopOffsetProperty = SimpleIntegerProperty(definition?.loopOffset ?: -1)
    val leftHandItemProperty = SimpleIntegerProperty(definition?.leftHandItem ?: -1)
    val rightHandItemProperty = SimpleIntegerProperty(definition?.rightHandItem ?: -1)

    fun getSkeletons(): ObservableMap<Int, AnimationSkeleton> {
        if (!this::skeletons.isInitialized) {
            try {
                val skeletonsMap: Map<Int, AnimationSkeleton> = definition
                    ?.frameHashes
                    ?.map { getCacheSafe().getAnimationSkeletonDefinition(it) }
                    ?.distinctBy { it.id }
                    ?.associate { it.id to AnimationSkeleton("${it.id}", it) }
                    ?: emptyMap()
                skeletons = FXCollections.observableMap(skeletonsMap)
            } catch (e: Exception) {
                Qodat.logException("Could not get animation {${getName()}}'s skeletons", e)
                return FXCollections.emptyObservableMap()
            }
        }
        return skeletons
    }

    override fun getFrameList(): ObservableList<AnimationFrame> {
        if (!this::frames.isInitialized) {
            try {
                val framesArray = if (definition == null) emptyArray() else Array(definition.frameHashes.size) { idx ->
                    val frameDefinition = getCacheSafe().getFrameDefinition(definition.frameHashes[idx])!!
                    AnimationFrame(
                        name = "frame[$idx]",
                        definition = frameDefinition,
                        duration = definition.frameLengths[idx]
                    ).apply {
                        idProperty.set(this@Animation.definition.frameHashes[idx])
                    }
                }
                frames = FXCollections.observableArrayList(*framesArray)
            } catch (e: Exception) {
                Qodat.logException("Could not get animation {${getName()}}'s frames", e)
                return FXCollections.emptyObservableList()
            }
        }
        return frames
    }

    private fun getCacheSafe() = requireNotNull(cache)
    { "Cache must not be null if loading from cache definition!" }

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

    override fun getName() = nameProperty.get()

    override fun getViewNode(): Node {
        if (!this::viewBox.isInitialized) {
            viewBox = LabeledHBox(labelProperty, labelPrefix = "animation").apply {
                nameProperty.bind(editableValueProperty.stringBinding(labelProperty) {
                    "${editableValueProperty.get()} (${labelProperty.get()})"
                })
                contextmenu {
                    item("Rename") {
                        setOnAction {
                            editableProperty.set(true)
                        }
                    }
                    menu("export") {
                        menuItem("GIF") {
                            BackgroundTasks.submit(
                                addProgressIndicator = true,
                                AnimationToGifTask(
                                    exportPath = Properties.defaultExportsPath.get(),
                                    scene = SubScene3D.subSceneProperty.get(),
                                    animationPlayer = SubScene3D.contextProperty.get().animationPlayer,
                                    animation = this@Animation
                                )
                            )
                        }
                        menuItem("mp4") {
                            BackgroundTasks.submit(
                                addProgressIndicator = true,
                                AnimationToMp4Task(
                                    exportPath = Properties.defaultExportsPath.get(),
                                    scene = SubScene3D.subSceneProperty.get(),
                                    animationPlayer = SubScene3D.contextProperty.get().animationPlayer,
                                    animation = this@Animation
                                )
                            )
                        }
                    }
                }
            }
        }
        return viewBox
    }

    fun copy() = Animation(labelProperty.get(), definition, cache)

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
