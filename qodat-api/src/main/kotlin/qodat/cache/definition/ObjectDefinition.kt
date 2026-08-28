package qodat.cache.definition

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
interface ObjectDefinition : AnimatedEntityDefinition {

    val modelSizeX: Int get() = 128
    val modelSizeHeight: Int get() = 128
    val modelSizeY: Int get() = 128
}