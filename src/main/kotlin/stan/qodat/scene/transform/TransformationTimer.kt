package stan.qodat.scene.transform

import javafx.animation.Animation
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.util.Duration
import stan.qodat.Qodat
import stan.qodat.util.FrameRateMeasurer
import stan.qodat.util.FrameTimeUtil

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
        transformer.setFrame(frameIndexProperty.get())
        transformer.update(transformableList)
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

        transformerProperty.addListener { _, _, animation ->

            Qodat.mainController.playBtn.selectedProperty().set(false)

            val wasPlaying = timeline.status == Animation.Status.RUNNING
            timeline.stop()
            timeline.keyFrames.clear()

            if (animation == null)
                return@addListener

            val frames = animation.getFrameList()
            var duration = Duration.ZERO
            for ((idx, frame) in frames.withIndex()) {
                val keyValue = KeyValue(frameIndexProperty, idx)
                val keyFrame = KeyFrame(duration, onFrameChangeEvent, keyValue)
                timeline.keyFrames.add(keyFrame)
                duration = duration.add(frame.getDuration())
            }

            if (wasPlaying || autoPlayOnChange.get()) {
                Qodat.mainController.playBtn.selectedProperty().set(true)
                timeline.play()
            }
        }
    }
}