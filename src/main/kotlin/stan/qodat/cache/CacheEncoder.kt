package stan.qodat.cache

import java.io.File
import java.io.UnsupportedEncodingException

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
interface CacheEncoder {

    @Throws(UnsupportedEncodingException::class)
    fun encode(format: Cache) : File
}