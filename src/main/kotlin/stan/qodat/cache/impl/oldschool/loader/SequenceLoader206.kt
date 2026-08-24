package stan.qodat.cache.impl.oldschool.loader

import net.runelite.cache.io.InputStream
import stan.qodat.cache.impl.oldschool.definition.SequenceDefinition206

/**
 * Updated loader based of RuneLite's [net.runelite.cache.definitions.loaders.SequenceLoader].
 *
 * @author Stan van der Bend
 */
class SequenceLoader206 {

    fun load(id: Int, b: ByteArray): SequenceDefinition206 {
        val def = SequenceDefinition206(id.toString())
        val `is` = InputStream(b)
        while (true) {
            val opcode = `is`.readUnsignedByte()
            if (opcode == 0) {
                break
            }
            def.decodeValues(opcode, `is`)
        }
        return def
    }

    private fun SequenceDefinition206.decodeValues(opcode: Int, stream: InputStream) {
        val length: Int
        when (opcode) {
            1 -> {
                val (lengths, ids) = stream.readFrameLengthAndIdTables()
                frameLenghts = lengths
                frameIDs = ids
            }

            2 -> frameStep = stream.readUnsignedShort()
            3 -> {
                length = stream.readUnsignedByte()
                interleaveLeave = IntArray(1 + length) {
                    if (it == length)
                        9999999
                    else
                        stream.readUnsignedByte()
                }
            }

            4 -> stretches = true
            5 -> forcedPriority = stream.readUnsignedByte()
            6 -> leftHandItem = stream.readUnsignedShort()
            7 -> rightHandItem = stream.readUnsignedShort()
            8 -> maxLoops = stream.readUnsignedByte()
            9 -> precedenceAnimating = stream.readUnsignedByte()
            10 -> priority = stream.readUnsignedByte()
            11 -> replyMode = stream.readUnsignedByte()
            12 -> chatFrameIds = stream.readPackedArchiveFileIds()

            13 -> frameSounds = IntArray(stream.readUnsignedByte()) {
                stream.read24BitInt()
            }

            14 -> {
                animMayaId = stream.readInt()
            }

            15 -> {
                repeat(stream.readUnsignedShort()) {
                    stream.readUnsignedShort()
                    stream.read24BitInt()
                }
            }

            16 -> {
                stream.readUnsignedShort()
                stream.readUnsignedShort()
            }

            17 -> {
                repeat(stream.readUnsignedByte()) {
                    stream.readUnsignedByte()
                }
            }

            18 -> {
                name = stream.readString()
            }

            19 -> Unit
            else -> Unit
        }
    }
}

