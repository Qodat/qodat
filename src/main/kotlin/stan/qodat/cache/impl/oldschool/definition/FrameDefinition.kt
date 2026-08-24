package stan.qodat.cache.impl.oldschool.definition

import qodat.cache.definition.AnimationFrameLegacyDefinition
import qodat.cache.definition.AnimationTransformationGroup

/**
 * Legacy animation frame (index 0). OSRS uses a mask stream plus smart
 * deltas; NR 317 uses magic `0xF9 0xF9` and `RSBuffer.getShort2`.
 */
class FrameDefinition : AnimationFrameLegacyDefinition {

    var id: Int = -1
    lateinit var framemap: FramemapDefinition
    var translatorCount: Int = -1
    var indexFrameIds: IntArray = intArrayOf()
    var translator_x: IntArray = intArrayOf()
    var translator_y: IntArray = intArrayOf()
    var translator_z: IntArray = intArrayOf()
    var showing: Boolean = false

    override val transformationCount: Int
        get() = translatorCount
    override val transformationGroupAccessIndices: IntArray
        get() = indexFrameIds
    override val transformationDeltaX: IntArray
        get() = translator_x
    override val transformationDeltaY: IntArray
        get() = translator_y
    override val transformationDeltaZ: IntArray
        get() = translator_z
    override val transformationGroup: AnimationTransformationGroup
        get() = framemap
}
