package qodat.cache.definition

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
interface AnimationDefinition {

    val id: String
    val frameHashes: IntArray
    val frameLengths : IntArray
    val loopOffset: Int
    val leftHandItem: Int
    val rightHandItem: Int
    val skeletalAnimationId: Int
    fun isMayaAnimation(): Boolean = skeletalAnimationId >= 0
}