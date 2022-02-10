package qodat.cache.definition

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   30/01/2021
 */
interface TextureDefinition {
    var id: Int
    val fileIds: IntArray
    var pixels: IntArray
}