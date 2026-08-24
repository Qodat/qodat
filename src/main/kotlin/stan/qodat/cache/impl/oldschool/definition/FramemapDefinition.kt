package stan.qodat.cache.impl.oldschool.definition

import qodat.cache.definition.AnimationTransformationGroup

/**
 * Skeleton / framemap (index 1). OSRS values are unsigned bytes;
 * NR 317 values are unsigned shorts, gated by trailing `0xF9 0xF9`.
 */
class FramemapDefinition : AnimationTransformationGroup {

    override var id: Int = -1
    var length: Int = 0
    var types: IntArray = intArrayOf()
    var frameMaps: Array<IntArray> = emptyArray()

    override val transformationTypes: IntArray
        get() = types
    override val targetVertexGroupsIndices: Array<IntArray>
        get() = frameMaps
}
