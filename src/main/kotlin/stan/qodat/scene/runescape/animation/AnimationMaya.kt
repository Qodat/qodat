package stan.qodat.scene.runescape.animation

import jagex.MayaAnimation
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.runelite.cache.IndexType
import net.runelite.cache.fs.Index
import qodat.cache.Cache
import qodat.cache.definition.AnimationMayaDefinition
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.util.runCatchingWithDialog

class AnimationMaya(label: String, override val definition: AnimationMayaDefinition, cache: Cache? = null) :
    Animation(label, definition, cache) {

    private val frames = FXCollections.observableArrayList<AnimationFrame>()

    override fun getFrameList(): ObservableList<AnimationFrame> {
        if (frames.isEmpty()) {
            val animationsArchive: Index = OldschoolCacheRuneLite.store.getIndex(IndexType.ANIMATIONS)
            val framesArchive = OldschoolCacheRuneLite.store.getIndex(IndexType.SKELETONS)
            val mayaAnimation = MayaAnimation.loadMayaAnimation(
                animationsArchive,
                framesArchive,
                definition.animMayaID,
                false
            )
            runCatchingWithDialog("Loading Maya Animation") {
                runBlocking {
                    withTimeout(5000) {
                        while (!mayaAnimation.isAnimationLoaded) {
                            Thread.sleep(100)
                        }
                    }
                }
            }
            val animMayaDuration = definition.animMayaEnd - definition.animMayaStart
            repeat(animMayaDuration) { index ->
                val frame = AnimationFrameMaya(
                    name = "frame[$index]",
                    duration = 1, // TODO: figure out duration
                    index = index,
                    animation = mayaAnimation,
                )
                frames.add(frame)
            }
        }
        return frames
    }

    override fun copy(): AnimationLegacy =
        AnimationLegacy(labelProperty.get(), definition, cache)

    override fun exportAsMp4() = TODO()
    override fun exportAsGif() = TODO()
}
