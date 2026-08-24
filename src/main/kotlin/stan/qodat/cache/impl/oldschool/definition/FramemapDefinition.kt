package stan.qodat.cache.impl.oldschool.definition

import qodat.cache.definition.AnimationTransformationGroup
import stan.qodat.cache.impl.oldschool.loader.AnimationFrameCodec

/**
 * Skeleton / framemap (index 1). OSRS values are unsigned bytes;
 * NR 317 values are unsigned shorts, gated by trailing `0xF9 0xF9`.
 */
class FramemapDefinition : AnimationTransformationGroup {

    override var id: Int = -1
    var length: Int = 0
    var types: IntArray = AnimationFrameCodec.EMPTY_INTS
    var frameMaps: Array<IntArray> = AnimationFrameCodec.EMPTY_INT_ARRAYS

    override val transformationTypes: IntArray
        get() = types
    override val targetVertexGroupsIndices: Array<IntArray>
        get() = frameMaps
}
