package stan.qodat.scene.transform

import stan.qodat.scene.runescape.animation.AnimationFrame

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
interface Transformable {

    fun animate(frame: AnimationFrame)

}