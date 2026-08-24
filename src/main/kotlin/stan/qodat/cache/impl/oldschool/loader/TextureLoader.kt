package stan.qodat.cache.impl.oldschool.loader

import com.displee.io.impl.InputBuffer
import stan.qodat.cache.impl.oldschool.definition.TextureDefinition

/**
 * OSRS texture (index 9, archive 0) decoder.
 *
 * The compact rev233 single-file payload and the older multi-file table coexist
 * so a newer decoder reads older caches. Layout is selected from remaining
 * bytes (`[REV233_PAYLOAD_SIZE]` → compact; otherwise legacy).
 */
class TextureLoader {

    fun load(id: Int, b: ByteArray): TextureDefinition {
        val def = TextureDefinition(id)
        val input = InputBuffer(b)
        if (input.remaining() == REV233_PAYLOAD_SIZE) {
            decodeRev233(def, input)
        } else {
            decodeLegacy(def, input)
        }
        return def
    }

    private fun decodeRev233(def: TextureDefinition, input: InputBuffer) {
        def.fileIds = intArrayOf(input.readUnsignedShort())
        def.missingColor = input.readUnsignedShort()
        def.field1778 = input.readUnsignedByte() == 1
        def.animationDirection = input.readUnsignedByte()
        def.animationSpeed = input.readUnsignedByte()
    }

    private fun decodeLegacy(def: TextureDefinition, input: InputBuffer) {
        def.missingColor = input.readUnsignedShort()
        def.field1778 = input.readByte().toInt() != 0
        val count = input.readUnsignedByte()
        def.fileIds = IntArray(count) { input.readUnsignedShort() }
        if (count > 1) {
            def.field1780 = IntArray(count - 1) { input.readUnsignedByte() }
            def.field1781 = IntArray(count - 1) { input.readUnsignedByte() }
        }
        def.field1786 = IntArray(count) { input.readInt() }
        def.animationDirection = input.readUnsignedByte()
        def.animationSpeed = input.readUnsignedByte()
    }

    companion object {
        const val REV233_PAYLOAD_SIZE = 7
    }
}
