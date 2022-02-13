package stan.qodat.scene.provider

import javafx.scene.Node

/**
 * Represents an interface that can be added to a 3D scene.
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   30/01/2021
 */
interface SceneNodeProvider {

    fun getSceneNode() : Node
}