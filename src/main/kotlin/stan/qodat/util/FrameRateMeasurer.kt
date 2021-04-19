package stan.qodat.util

import javafx.beans.property.SimpleStringProperty
import java.util.*

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class FrameRateMeasurer {

    /**
     * Holds a series of time stamps between consecutive frames.
     */
    private val frameTimes = LongArray(100)

    /**
     * The current index of the [frameTimes] array.
     */
    private var frameTimeIndex = 0

    private var filledFrameTimesArray = false

    fun measure(now: Long) : OptionalInt {
        val previousTime = frameTimes[frameTimeIndex]
        frameTimes[frameTimeIndex] = now
        frameTimeIndex = (frameTimeIndex + 1) % frameTimes.size
        if (frameTimeIndex == 0)
            filledFrameTimesArray = true
        if (filledFrameTimesArray){
            val elapsedNanos = now - previousTime
            val elapsedNanosPerFrame = elapsedNanos / frameTimes.size
            val frameRate = 1_000_000_000.0 / elapsedNanosPerFrame
            return OptionalInt.of(frameRate.toInt())
        }
        return OptionalInt.empty()
    }
}