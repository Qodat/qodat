package stan.qodat.cache.impl.oldschool.loader

import com.displee.io.impl.OutputBuffer
import stan.qodat.cache.impl.oldschool.definition.SpotAnimDefinition

/**
 * OSRS spotanim (config archive 13) decoder/encoder.
 *
 * Short model ids (opcode 1) and int model ids (opcode 3) coexist so a newer
 * decoder reads older payloads. Opcode 9 (debug name) is newest-only. Unknown
 * opcodes are ignored. There is no archive-revision gate.
 */
class SpotAnimLoader {

    fun load(id: Int, b: ByteArray): SpotAnimDefinition {
        val def = SpotAnimDefinition(id)
        DecodeCursor(b).forEachOpcode { opcode -> def.decodeValues(opcode, this) }
        return def
    }

    fun encode(def: SpotAnimDefinition, format: SpotAnimEncodeFormat = SpotAnimEncodeFormat.LATEST): ByteArray {
        val out = OutputBuffer(16)
        writeModel(out, def.modelId, format)
        if (def.animationId != -1) {
            out.writeByte(2)
            out.writeShort(def.animationId)
        }
        if (def.resizeX != 128) {
            out.writeByte(4)
            out.writeShort(def.resizeX)
        }
        if (def.resizeY != 128) {
            out.writeByte(5)
            out.writeShort(def.resizeY)
        }
        if (def.rotation != 0) {
            out.writeByte(6)
            out.writeShort(def.rotation)
        }
        if (def.ambient != 0) {
            out.writeByte(7)
            out.writeByte(def.ambient)
        }
        if (def.contrast != 0) {
            out.writeByte(8)
            out.writeByte(def.contrast)
        }
        val debugName = def.debugName
        if (format == SpotAnimEncodeFormat.LATEST && debugName != null) {
            out.writeByte(9)
            out.writeString(debugName)
        }
        writePairs(out, 40, def.recolorToFind, def.recolorToReplace)
        writePairs(out, 41, def.textureToFind, def.textureToReplace)
        out.writeByte(0)
        return out.array()
    }

    private fun SpotAnimDefinition.decodeValues(opcode: Int, stream: DecodeCursor) {
        when (opcode) {
            1 -> modelId = stream.readUnsignedShort()
            2 -> animationId = stream.readUnsignedShort()
            3 -> modelId = stream.readInt()
            4 -> resizeX = stream.readUnsignedShort()
            5 -> resizeY = stream.readUnsignedShort()
            6 -> rotation = stream.readUnsignedShort()
            7 -> ambient = stream.readUnsignedByte()
            8 -> contrast = stream.readUnsignedByte()
            9 -> debugName = stream.readString()
            40 -> {
                val length = stream.readUnsignedByte()
                val find = ShortArray(length)
                val replace = ShortArray(length)
                for (i in 0 until length) {
                    find[i] = stream.readUnsignedShort().toShort()
                    replace[i] = stream.readUnsignedShort().toShort()
                }
                recolorToFind = find
                recolorToReplace = replace
            }
            41 -> {
                val length = stream.readUnsignedByte()
                val find = ShortArray(length)
                val replace = ShortArray(length)
                for (i in 0 until length) {
                    find[i] = stream.readUnsignedShort().toShort()
                    replace[i] = stream.readUnsignedShort().toShort()
                }
                textureToFind = find
                textureToReplace = replace
            }
            else -> Unit
        }
    }

    private fun writeModel(out: OutputBuffer, modelId: Int, format: SpotAnimEncodeFormat) {
        if (modelId == 0) return
        when (format) {
            SpotAnimEncodeFormat.SHORT_MODEL -> {
                require(modelId in 0..0xFFFF) { "modelId $modelId does not fit opcode 1" }
                out.writeByte(1)
                out.writeShort(modelId)
            }
            SpotAnimEncodeFormat.LATEST -> {
                out.writeByte(3)
                out.writeInt(modelId)
            }
        }
    }

    private fun writePairs(out: OutputBuffer, opcode: Int, find: ShortArray?, replace: ShortArray?) {
        if (find == null || replace == null || find.isEmpty()) return
        out.writeByte(opcode)
        out.writeByte(find.size)
        for (i in find.indices) {
            out.writeShort(find[i].toInt() and 0xFFFF)
            out.writeShort(replace[i].toInt() and 0xFFFF)
        }
    }
}

enum class SpotAnimEncodeFormat {
    /** Oldest interesting payload: opcode 1 short model, no debug name. */
    SHORT_MODEL,
    /** Current client table: opcode 3 int model, opcode 9 when [SpotAnimDefinition.debugName] is set. */
    LATEST,
}
