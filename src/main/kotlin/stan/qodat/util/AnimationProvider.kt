package stan.qodat.util

import stan.qodat.scene.runescape.animation.Animation

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   31/01/2021
 */
interface AnimationProvider {

    fun getAnimations() : Array<Animation>
}