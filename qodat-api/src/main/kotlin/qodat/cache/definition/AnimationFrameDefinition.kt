package qodat.cache.definition

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
interface AnimationFrameDefinition {

    val transformationCount : Int
    val transformationGroupAccessIndices : IntArray
    val transformationDeltaX : IntArray
    val transformationDeltaY : IntArray
    val transformationDeltaZ : IntArray
    val framemapArchiveIndex: Int

    val transformationGroup : AnimationTransformationGroup
}
