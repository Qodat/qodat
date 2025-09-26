package stan.qodat.scene.runescape.entity

import qodat.cache.Cache
import qodat.cache.EncodeResult
import qodat.cache.Encoder
import qodat.cache.definition.NPCDefinition
import stan.qodat.Properties
import stan.qodat.cache.impl.displee.DispleeCache
import stan.qodat.scene.runescape.animation.Animation
import java.io.UnsupportedEncodingException

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class NPC(
    cache: Cache = DispleeCache,
    definition: NPCDefinition,
    animationProvider: NPCDefinition.() -> Array<Animation>
) : AnimatedEntity<NPCDefinition>(cache, definition, animationProvider, "npc"), Encoder {

    override fun encode(format: Cache) : EncodeResult {
        throw UnsupportedEncodingException()
    }

    override fun toString(): String = getName()

    override fun property() = Properties.selectedNpcName
}
