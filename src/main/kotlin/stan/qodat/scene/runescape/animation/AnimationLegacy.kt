package stan.qodat.scene.runescape.animation

import javafx.beans.property.SimpleIntegerProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.ObservableMap
import qodat.cache.Cache
import qodat.cache.Encoder
import qodat.cache.definition.AnimationDefinition
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.control.export.gif.AnimationToGifTask
import stan.qodat.scene.control.export.mp4.AnimationToMp4Task
import stan.qodat.task.BackgroundTasks


class AnimationLegacy(
    label: String,
    definition: AnimationDefinition? = null,
    cache: Cache? = null
) : Encoder, Animation(label, definition, cache) {

    private lateinit var frames: ObservableList<AnimationFrame>
    private lateinit var skeletons: ObservableMap<Int, AnimationSkeleton>

    val exportFrameArchiveId = SimpleIntegerProperty()

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
                val framesArray = if (definition == null)
                    emptyArray()
                else {
                    Array(definition.frameHashes.size) { idx ->
                        val frameDefinition = getCacheSafe().getFrameDefinition(definition.frameHashes[idx])!!
                        AnimationFrameLegacy(
                            name = "frame[$idx]",
                            definition = frameDefinition,
                            duration = definition.frameLengths[idx]
                        ).apply {
                            idProperty.set(this@AnimationLegacy.definition.frameHashes[idx])
                        }
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

    override fun copy() = AnimationLegacy(labelProperty.get(), definition, cache)

    override fun exportAsMp4() {
        BackgroundTasks.submit(
            addProgressIndicator = true,
            AnimationToMp4Task(
                exportPath = Properties.defaultExportsPath.get(),
                scene = SubScene3D.subSceneProperty.get(),
                animationPlayer = SubScene3D.contextProperty.get().animationPlayer,
                animation = this
            )
        )
    }

    override fun exportAsGif() {
        BackgroundTasks.submit(
            addProgressIndicator = true,
            AnimationToGifTask(
                exportPath = Properties.defaultExportsPath.get(),
                scene = SubScene3D.subSceneProperty.get(),
                animationPlayer = SubScene3D.contextProperty.get().animationPlayer,
                animation = this
            )
        )
    }
}
