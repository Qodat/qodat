package stan.qodat.scene.runescape.model

import stan.qodat.util.SceneNodeProvider

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   30/01/2021
 */
interface ModelSkin : SceneNodeProvider {

    /**
     * Should update all points in the skin.
     *
     * @param skeleton the [ModelSkeleton] to retrieve x,y,z values from for each local vertex.
     */
    fun updatePoints(skeleton: ModelSkeleton)
}