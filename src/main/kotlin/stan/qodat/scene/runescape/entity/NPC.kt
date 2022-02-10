package stan.qodat.scene.runescape.entity

import stan.qodat.Properties
import qodat.cache.Cache
import qodat.cache.EncodeResult
import qodat.cache.Encoder
import qodat.cache.definition.NPCDefinition
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.scene.runescape.animation.Animation
import java.io.UnsupportedEncodingException

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class NPC(
    cache: Cache = OldschoolCacheRuneLite,
    definition: NPCDefinition,
    animationProvider: NPCDefinition.() -> Array<Animation>
) : AnimatedEntity<NPCDefinition>(cache, definition, animationProvider), Encoder {

    override fun encode(format: Cache) : EncodeResult {
        throw UnsupportedEncodingException()
    }

    override fun toString(): String = getName()

    override fun property() = Properties.selectedNpcName
}