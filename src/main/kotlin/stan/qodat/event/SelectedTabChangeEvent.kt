package stan.qodat.event

import javafx.event.Event
import javafx.event.EventType
import javafx.scene.Node

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class SelectedTabChangeEvent(val selected: Boolean, val otherSelected: Boolean) : Event(EVENT_TYPE) {

    companion object {
        @JvmStatic
        val EVENT_TYPE = EventType<SelectedTabChangeEvent>("Selected Tab Change")
    }
}