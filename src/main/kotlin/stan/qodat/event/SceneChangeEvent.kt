package stan.qodat.event

import javafx.event.Event
import javafx.event.EventType
import stan.qodat.scene.transform.Transformable

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class SceneChangeEvent(val added: Boolean) : Event(EVENT_TYPE) {

    companion object {
        @JvmStatic
        val EVENT_TYPE = EventType<SceneChangeEvent>("Scene Change")
    }
}