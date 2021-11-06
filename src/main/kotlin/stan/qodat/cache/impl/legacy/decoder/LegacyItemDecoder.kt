package stan.qodat.cache.impl.legacy.decoder

import net.runelite.cache.io.InputStream
import stan.qodat.cache.impl.legacy.LegacyItemDefinition

/**
 * TODO: add documentation
 *
 * @author Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @version 1.0
 * @since 2019-01-11
 */
class LegacyItemDecoder {

    fun load(id: Int, `is`: InputStream): LegacyItemDefinition {
        // TODO
        return LegacyItemDefinition(
            id.toString(),
            emptyArray(),
        )
    }
}

