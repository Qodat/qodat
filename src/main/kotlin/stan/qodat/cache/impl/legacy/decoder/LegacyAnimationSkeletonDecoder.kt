package stan.qodat.cache.impl.legacy.decoder

import net.runelite.cache.io.InputStream
import stan.qodat.cache.impl.legacy.LegacyAnimationSkeletonDefinition

/**
 * TODO: add documentation
 *
 * @author Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @version 1.0
 * @since 2019-01-08
 */
class LegacyAnimationSkeletonDecoder {

    fun load(fileId: Int, `in`: InputStream): LegacyAnimationSkeletonDefinition {
        val length = `in`.readUnsignedShort()
        return LegacyAnimationSkeletonDefinition(
            id = fileId,
            transformationTypes = IntArray(length) {
                `in`.readUnsignedShort()
            },
            targetVertexGroupsIndices = Array(length) {
                `in`.readUnsignedShort()
            }.map {
                IntArray(it) {
                    `in`.readUnsignedShort()
                }
            }.toTypedArray()
        )
    }
}
