package stan.qodat.scene.runescape.entity

import qodat.cache.Cache
import qodat.cache.EncodeResult
import qodat.cache.Encoder
import qodat.cache.definition.ObjectDefinition
import stan.qodat.Properties
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.scene.runescape.animation.Animation
import java.io.UnsupportedEncodingException

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class Object(
    cache: Cache = OldschoolCacheRuneLite,
    definition: ObjectDefinition,
    animationProvider: ObjectDefinition.() -> Array<Animation>
) : AnimatedEntity<ObjectDefinition>(cache, definition, animationProvider, "loc"), Encoder {

    override fun encode(format: Cache) : EncodeResult {
        throw UnsupportedEncodingException()
    }

    override fun property() = Properties.selectedObjectName
}
