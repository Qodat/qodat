package stan.qodat.cache.impl.legacy.decoder

import net.runelite.cache.io.InputStream
import stan.qodat.cache.impl.legacy.LegacyAnimationDefinition

/**
 * TODO: add documentation
 *
 * @author Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @version 1.0
 * @since 2019-01-08
 */
class LegacySequenceDecoder {

    fun load(id: Int, `is`: InputStream): LegacyAnimationDefinition {

        val def = LegacyAnimationBuilder()

        def.id = id

        while (true) {
            val opcode = `is`.readUnsignedByte()

            if (opcode == 0)
                break

            this.decodeValues(opcode, def, `is`)
        }

        if (def.leftHandItem == 65535)
            def.leftHandItem = 0
        if (def.rightHandItem == 65535)
            def.rightHandItem = 0

        return LegacyAnimationDefinition(
            id = id.toString(),
            frameHashes =  def.frameIDs?: IntArray(0),
            frameLengths = def.frameLenghts?: IntArray(0)
        )
    }

    private fun decodeValues(opcode: Int, def: LegacyAnimationBuilder, stream: InputStream) {

        when (opcode) {
            1 -> {
                val frameCount = stream.readUnsignedShort()
                def.frameIDs = IntArray(frameCount) {
                    stream.readInt()
                }
                def.frameLenghts = IntArray(frameCount) {
                    stream.readUnsignedByte()
                }
            }
            2 -> stream.readUnsignedShort()
            3 -> {
                val k = stream.readUnsignedByte()
                for (l in 0 until k)
                    stream.readUnsignedByte()
            }
            5 -> def.forcedPriority = stream.readUnsignedByte()
            6 -> def.leftHandItem = stream.readUnsignedShort()
            7 -> def.rightHandItem = stream.readUnsignedShort()
            8 -> def.maxLoops = stream.readUnsignedByte()
            9 -> def.forcedPriority = stream.readUnsignedByte()
            10 -> def.priority = stream.readUnsignedByte()
            11 -> def.replyMode = stream.readUnsignedByte()
            12 -> stream.readInt()
            else -> println("Error unrecognised seq config code: $opcode")
        }
    }

    private companion object {
        private class LegacyAnimationBuilder {
            var id: Int = 0
            var frameIDs: IntArray? = null // top 16 bits are FrameDefinition ids
            var field3048: IntArray? = null
            var frameLenghts: IntArray? = null
            var rightHandItem = -1
            var interleaveLeave: IntArray? = null
            var stretches = false
            var forcedPriority = 5
            var maxLoops = 99
            var field3056: IntArray? = null
            var precedenceAnimating = -1
            var leftHandItem = -1
            var replyMode = 2
            var frameStep = -1
            var priority = -1
        }
    }
}
