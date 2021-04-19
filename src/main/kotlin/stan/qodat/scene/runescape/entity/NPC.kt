package stan.qodat.scene.runescape.entity

import stan.qodat.cache.Cache
import stan.qodat.cache.CacheEncoder
import stan.qodat.cache.definition.NPCDefinition
import stan.qodat.cache.impl.oldschool.OldschoolCache
import java.io.UnsupportedEncodingException

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class NPC(
    cache: Cache = OldschoolCache,
    private val definition: NPCDefinition
) : AnimatedEntity<NPCDefinition>(cache, definition), CacheEncoder {

    override fun encode(format: Cache) {
        throw UnsupportedEncodingException()
    }
}