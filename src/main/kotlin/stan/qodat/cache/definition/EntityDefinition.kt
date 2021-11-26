package stan.qodat.cache.definition

import java.util.*

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   29/01/2021
 */
interface EntityDefinition {

    fun getOptionalId(): OptionalInt = OptionalInt.empty()

    val name: String
    val modelIds: Array<String>

    val findColor: ShortArray?
    val replaceColor: ShortArray?
}