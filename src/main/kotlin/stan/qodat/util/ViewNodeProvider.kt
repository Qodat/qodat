package stan.qodat.util

import javafx.scene.Node

/**
 * Represents an interface that can be added to GUI elements.
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   30/01/2021
 */
interface ViewNodeProvider {

    fun getViewNode() : Node
}