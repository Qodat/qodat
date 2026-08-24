package stan.qodat.scene.runescape.animation

import jagex.MayaAnimation
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import qodat.cache.Cache
import qodat.cache.definition.AnimationMayaDefinition
import stan.qodat.Properties
import stan.qodat.cache.impl.displee.CacheIndex
import stan.qodat.cache.impl.displee.DispleeCache
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.control.export.gif.AnimationToGifTask
import stan.qodat.task.BackgroundTasks
import stan.qodat.util.runCatchingWithDialog

class AnimationMaya(
    label: String,
    override val definition: AnimationMayaDefinition,
    cache: Cache? = null,
    numericId: Int = 0,
) : Animation(label, definition, cache, numericId) {

    private val frames = FXCollections.observableArrayList<AnimationFrame>()

    override fun getFrameList(): ObservableList<AnimationFrame> {
        if (frames.isEmpty()) {
            val rev229 = DispleeCache.store.index(CacheIndex.MODELS).revision >= 969
            val animationsArchive = if (rev229)
                DispleeCache.store.index(CacheIndex.MAYA_ANIMATIONS)
            else
                DispleeCache.store.index(CacheIndex.ANIMATIONS)
            val framesArchive =
                DispleeCache.store.index(CacheIndex.SKELETONS)
            val mayaAnimation = MayaAnimation.load(
                animationsArchive,
                framesArchive,
                definition.animMayaID,
                false
            ) ?: return frames
            runCatchingWithDialog("Loading Maya Animation") {
                mayaAnimation.awaitLoaded(5000)
            }
            val clipStart = definition.animMayaStart
            val clipLength = definition.animMayaEnd - clipStart
            val animMayaDuration = if (clipLength > 0) clipLength else mayaAnimation.playbackLength
            repeat(animMayaDuration) { index ->
                val step = clipStart + index
                val frame = AnimationFrameMaya(
                    name = "frame[$step]",
                    duration = 1,
                    index = step,
                    animation = mayaAnimation,
                )
                frames.add(frame)
            }
        }
        return frames
    }

    override fun copy(): AnimationLegacy =
        AnimationLegacy(getName(), definition, cache, numericId())

    override fun exportAsMp4() = TODO()

    override fun exportAsGif() {
        BackgroundTasks.submit(
            addProgressIndicator = true,
            AnimationToGifTask(
                exportPath = Properties.defaultExportsPath.get().resolve("gifs"),
                scene = SubScene3D.subSceneProperty.get(),
                animationPlayer = SubScene3D.contextProperty.get().animationPlayer,
                animation = this
            )
        )
    }
}
