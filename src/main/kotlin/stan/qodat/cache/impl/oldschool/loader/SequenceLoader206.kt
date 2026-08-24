package stan.qodat.cache.impl.oldschool.loader

import stan.qodat.cache.impl.oldschool.definition.SequenceCommonFields
import stan.qodat.cache.impl.oldschool.definition.SequenceDefinition206

/**
 * Updated loader based of RuneLite's [net.runelite.cache.definitions.loaders.SequenceLoader].
 *
 * @author Stan van der Bend
 */
class SequenceLoader206 {

    private val cursor = DecodeCursor(ByteArray(0))

    fun load(id: Int, b: ByteArray): SequenceDefinition206 {
        val def = SequenceDefinition206(id.toString())
        cursor.reset(b)
        cursor.forEachOpcode { opcode -> def.decodeValues(opcode, this) }
        return def
    }

    private fun SequenceDefinition206.decodeValues(opcode: Int, stream: DecodeCursor) {
        if (applySharedSequenceOpcode(opcode, stream)) return
        when (opcode) {
            13 -> {
                val count = stream.readUnsignedByte()
                frameSounds = if (count == 0) SequenceCommonFields.EMPTY_INTS
                else IntArray(count) { stream.read24BitInt() }
            }
            14 -> animMayaId = stream.readInt()
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
            else -> Unit
        }
    }
}
