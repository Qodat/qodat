package stan.qodat.scene.transform

import javafx.animation.Animation
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.beans.InvalidationListener
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Group
import javafx.scene.Node
import javafx.util.Duration
import stan.qodat.Properties
import stan.qodat.Qodat
import stan.qodat.cache.impl.qodat.QodatCache
import stan.qodat.javafx.onChange
import stan.qodat.scene.SubScene3D
import stan.qodat.scene.runescape.entity.AnimatedEntity
import stan.qodat.scene.runescape.entity.NPC
import stan.qodat.util.FrameRateMeasurer
import stan.qodat.util.FrameTimeUtil
import stan.qodat.util.setAndBind
import java.util.*

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
open class TransformationTimer<R : Transformer<*>> {

    /**
     * Measures the frame rate of this timer.
     */
    private val frameRateMeasurer = FrameRateMeasurer()

    /**
     * Represents a timeline composed of frames, where each frame is a [Transformable].
     */
    private val timeline = Timeline()

    /**
     * Holds a string representing the frame-rate of this timer.
     */
    val frameRateProperty = SimpleStringProperty()

    /**
     * Holds an [Transformer] that is sequenced every frame.
     */
    val transformerProperty = SimpleObjectProperty<R>()

    /**
     * Should the [timeline] start or continue playing if a new [transformer][R] is loaded in [transformerProperty].
     */
    val autoPlayOnChange = SimpleBooleanProperty(true)

    /**
     * Contains all [frames][Transformable].
     */
    val transformableList = FXCollections.observableArrayList<Transformable>()

    /**
     * Holds the frame currently being shown.
     */
    val frameIndexProperty = SimpleIntegerProperty()

    private val onFrameChangeEvent = EventHandler<ActionEvent> {
        frameRateMeasurer.measure(System.nanoTime()).ifPresent { fps ->
            frameRateProperty.set(String.format("FPS: %d", fps))
        }
        val transformer = transformerProperty.get() ?: return@EventHandler


//        val dragon = Qodat.mainController.viewerController.npcs.find { it.definition.getOptionalId().orElseGet { -1 } == 263 }
//        if (dragon != null) {
//            for (anim in dragon.getAnimations()){
//                if (anim.idProperty.get() == 4635) {
//                    anim.setFrame(frameIndexProperty.get())
//                    anim.update(listOf(dragon))
//                    break
//                }
//            }
//        }
        if (transformableList.size > 1){
            transformableList.forEach {
                if (it is GroupableTransformable) {
                    it.animate(frameIndexProperty.get())
                }
            }
        } else {
            transformer.setFrame(frameIndexProperty.get())
            transformer.update(transformableList)
        }
//        for (transformable in transformableList){
//            if (transformable is NPC) {
//                if (transformable.getName().contains("dragon", true)){
//                    for (anim in transformable.getAnimations()){
//                        if (anim.getName().contains("7870")){
//                            anim.setFrame(frameIndexProperty.get())
//                            anim.update(listOf(transformable))
//                            break
//                        }
//                    }
//                } else if (transformable.getName().contains("necromancer", true)){
//                    for (anim in transformable.getAnimations()){
//                        if (anim.getName().contains("1379")){
//                            anim.setFrame(frameIndexProperty.get())
//                            anim.update(listOf(transformable))
//                            break
//                        }
//                    }
//                }
//            }
//        }


//        println(transformableList)

    }

    fun jumpToFrame(frameIndex: Int) {
        val size = transformerProperty.get().getFrameList().size
        if (frameIndex >= size){
            println("Attempted to set frame index [$frameIndex] while size = $size (OUT OF BOUNDS)")
            return
        }
        val keyFrame = timeline.keyFrames[frameIndex]
        timeline.stop()
        timeline.jumpTo(keyFrame.time)
        frameIndexProperty.set(frameIndex)
        onFrameChangeEvent.handle(ActionEvent())
    }

    fun play() {
        timeline.play()
    }

    fun pause() {
        timeline.pause()
    }

    init {
        timeline.cycleCount = Timeline.INDEFINITE

        val invalidateFrameLengthListener = InvalidationListener {

            val wasPlaying = timeline.status == Animation.Status.RUNNING
            timeline.stop()
            timeline.keyFrames.clear()

            loadFramesInTimeline(transformerProperty.get().getFrameList())

            if (wasPlaying || autoPlayOnChange.get()) {
                Qodat.mainController.playBtn.selectedProperty().set(true)
                timeline.play()
            }
        }
        val updateFramesListener = ListChangeListener<TransformationGroup> {
            while (it.next()) {
                it.removed.forEach {
                    it.durationProperty().removeListener(invalidateFrameLengthListener)
                }
            }
            timeline.keyFrames.clear()
            loadFramesInTimeline(it.list)
        }

        transformerProperty.addListener { _, oldAnimation, animation ->

            oldAnimation?.getFrameList()?.run {
                forEach {
                    it.durationProperty().removeListener(invalidateFrameLengthListener)
                }
                removeListener(updateFramesListener)
            }

            Qodat.mainController.playBtn.selectedProperty().set(false)

            val wasPlaying = timeline.status == Animation.Status.RUNNING
            timeline.stop()
            timeline.keyFrames.clear()

            if (animation == null)
                return@addListener

            val frames = animation.getFrameList()
            frames.forEach {
                it.durationProperty().addListener(invalidateFrameLengthListener)
            }
            frames.addListener(updateFramesListener)
            loadFramesInTimeline(frames)

            if (wasPlaying || autoPlayOnChange.get()) {
                Qodat.mainController.playBtn.selectedProperty().set(true)
                timeline.play()
            }
        }
    }

    private fun loadFramesInTimeline(frames: ObservableList<out TransformationGroup>) {
        var duration = Duration.ZERO
        for ((idx, frame) in frames.withIndex()) {
            val keyValue = KeyValue(frameIndexProperty, idx)
            val keyFrame = KeyFrame(duration, onFrameChangeEvent, keyValue)
            timeline.keyFrames.add(keyFrame)
            duration = duration.add(frame.getDuration())
        }
    }
}