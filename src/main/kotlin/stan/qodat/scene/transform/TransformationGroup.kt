package stan.qodat.scene.transform

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.util.Duration

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   29/01/2021
 */
interface TransformationGroup {

    fun durationProperty() : SimpleObjectProperty<Duration>

    fun getDuration() : Duration
}