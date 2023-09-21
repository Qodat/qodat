package stan.qodat.cache.impl.legacy.decoder

import net.runelite.cache.io.InputStream
import stan.qodat.cache.impl.legacy.LegacyAnimationFrameDefinition
import stan.qodat.cache.impl.legacy.LegacyAnimationSkeletonDefinition

/**
 * TODO: add documentation
 *
 * @author Adam <Adam></Adam>@sigterm.info>
 * @author Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 *
 * @version 1.0
 * @since 2019-01-08
 */
class LegacyFrameDecoder {

    fun loadAll(framemap: LegacyAnimationSkeletonDefinition, `in`: InputStream) : Array<LegacyAnimationFrameDefinition?> {
        val count = `in`.readUnsignedShort()
        return Array(count * 3) {
            load(framemap, `in`)
        }
    }

    fun load(framemap: LegacyAnimationSkeletonDefinition, `in`: InputStream): LegacyAnimationFrameDefinition? {

        if(`in`.remaining() == 0)
            return null

        val id = `in`.readUnsignedShort()
        val length = `in`.readUnsignedByte()

        val indexFrameIds = IntArray(500)
        val scratchTranslatorX = IntArray(500)
        val scratchTranslatorY = IntArray(500)
        val scratchTranslatorZ = IntArray(500)

        var showing = false
        var lastI = -1
        var index = 0

        for (i in 0 until length) {

            val mask = `in`.readUnsignedByte()

            if (mask <= 0) continue

            if (framemap.transformationTypes[i] != 0) {
                for (var10 in i - 1 downTo lastI + 1) {
                    if (framemap.transformationTypes[var10] == 0) {
                        indexFrameIds[index] = var10
                        scratchTranslatorX[index] = 0
                        scratchTranslatorY[index] = 0
                        scratchTranslatorZ[index] = 0
                        ++index
                        break
                    }
                }
            }

            indexFrameIds[index] = i
            var var11: Short = 0

            if (framemap.transformationTypes[i] == 3) var11 = 128

            if (mask and 1 != 0) scratchTranslatorX[index] = `in`.readShort().toInt() else scratchTranslatorX[index] = var11.toInt()
            if (mask and 2 != 0) scratchTranslatorY[index] = `in`.readShort().toInt() else scratchTranslatorY[index] = var11.toInt()
            if (mask and 4 != 0) scratchTranslatorZ[index] = `in`.readShort().toInt() else scratchTranslatorZ[index] = var11.toInt()

            lastI = i
            ++index

            if (framemap.transformationTypes[i] == 5)
                showing = true
        }
        return LegacyAnimationFrameDefinition(
            showing,
            index,
            indexFrameIds,
            scratchTranslatorX,
            scratchTranslatorY,
            scratchTranslatorZ,
            framemap,
            id
        )
    }
}