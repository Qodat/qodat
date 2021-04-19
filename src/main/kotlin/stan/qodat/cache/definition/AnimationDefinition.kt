package stan.qodat.cache.definition

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
interface AnimationDefinition {

    val id: Int
    val frameHashes: IntArray
    val frameLengths : IntArray
}