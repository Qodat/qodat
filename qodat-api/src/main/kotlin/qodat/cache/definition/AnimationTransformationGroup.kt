package qodat.cache.definition

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
interface AnimationTransformationGroup {

    val id: Int
    val transformationTypes: IntArray
    val targetVertexGroupsIndices: Array<IntArray>
}
