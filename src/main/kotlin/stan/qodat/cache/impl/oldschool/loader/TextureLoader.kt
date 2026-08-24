package stan.qodat.cache.impl.oldschool.loader

import stan.qodat.cache.impl.oldschool.definition.TextureDefinition

/**
 * OSRS texture (index 9, archive 0) decoder.
 *
 * The compact rev233 single-file payload and the older multi-file table coexist
 * so a newer decoder reads older caches. Layout is selected from payload length
 * (`[REV233_PAYLOAD_SIZE]` → compact; otherwise legacy).
 *
 * Reads the [ByteArray] directly: the table is fixed-layout (no opcodes), and
 * [com.displee.io.impl.InputBuffer] would re-check bounds and endianness on
 * every short/int — the cost that showed up on the legacy multi-file bench.
 */
class TextureLoader {

    fun load(id: Int, b: ByteArray): TextureDefinition {
        val def = TextureDefinition(id)
        if (b.size == REV233_PAYLOAD_SIZE) {
            decodeRev233(def, b)
        } else {
            decodeLegacy(def, b)
        }
        return def
    }

    private fun decodeRev233(def: TextureDefinition, b: ByteArray) {
        def.fileIds = intArrayOf(uShort(b, 0))
        def.missingColor = uShort(b, 2)
        def.field1778 = (b[4].toInt() and 0xFF) == 1
        def.animationDirection = b[5].toInt() and 0xFF
        def.animationSpeed = b[6].toInt() and 0xFF
    }

    private fun decodeLegacy(def: TextureDefinition, b: ByteArray) {
        def.missingColor = uShort(b, 0)
        def.field1778 = b[2].toInt() != 0
        val count = b[3].toInt() and 0xFF
        var o = 4
        val fileIds = IntArray(count)
        for (i in 0 until count) {
            fileIds[i] = uShort(b, o)
            o += 2
        }
        def.fileIds = fileIds
        if (count > 1) {
            val extra = count - 1
            val field1780 = IntArray(extra)
            val field1781 = IntArray(extra)
            for (i in 0 until extra) {
                field1780[i] = b[o++].toInt() and 0xFF
            }
            for (i in 0 until extra) {
                field1781[i] = b[o++].toInt() and 0xFF
            }
            def.field1780 = field1780
            def.field1781 = field1781
        }
        val field1786 = IntArray(count)
        for (i in 0 until count) {
            field1786[i] = i32(b, o)
            o += 4
        }
        def.field1786 = field1786
        def.animationDirection = b[o].toInt() and 0xFF
        def.animationSpeed = b[o + 1].toInt() and 0xFF
    }

    companion object {
        const val REV233_PAYLOAD_SIZE = 7
    }
}

private fun uShort(b: ByteArray, o: Int): Int =
    (b[o].toInt() and 0xFF shl 8) or (b[o + 1].toInt() and 0xFF)

private fun i32(b: ByteArray, o: Int): Int =
    (b[o].toInt() shl 24) or
        (b[o + 1].toInt() and 0xFF shl 16) or
        (b[o + 2].toInt() and 0xFF shl 8) or
        (b[o + 3].toInt() and 0xFF)
