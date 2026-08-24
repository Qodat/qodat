package stan.qodat.cache.impl.oldschool.loader

import com.displee.io.impl.InputBuffer

/**
 * MSB cache-payload cursor. Jagex config bytes are always big-endian; this
 * skips [InputBuffer]'s per-byte bounds check and [InputBuffer.isMsb] branch
 * on every short/int (RuneLite reads the same way via `ByteBuffer.getShort`).
 */
internal class DecodeCursor(data: ByteArray) {
    private var data: ByteArray = data
    var offset = 0

    fun reset(newData: ByteArray) {
        data = newData
        offset = 0
    }

    fun remaining(): Int = data.size - offset

    fun raw(): ByteArray = data

    fun readUnsignedByte(): Int = data[offset++].toInt() and 0xFF

    fun readByte(): Byte = data[offset++]

    fun readUnsignedShort(): Int {
        val i = offset
        offset = i + 2
        return (data[i].toInt() and 0xFF shl 8) or (data[i + 1].toInt() and 0xFF)
    }

    fun readShort(): Int {
        var s = readUnsignedShort()
        if (s > 32767) s -= 65536
        return s
    }

    fun readInt(): Int {
        val i = offset
        offset = i + 4
        return (data[i].toInt() shl 24) or
            (data[i + 1].toInt() and 0xFF shl 16) or
            (data[i + 2].toInt() and 0xFF shl 8) or
            (data[i + 3].toInt() and 0xFF)
    }

    fun read24BitInt(): Int {
        val i = offset
        offset = i + 3
        return (data[i].toInt() and 0xFF shl 16) or
            (data[i + 1].toInt() and 0xFF shl 8) or
            (data[i + 2].toInt() and 0xFF)
    }

    fun readLong(): Long {
        val v = readInt().toLong() and 0xFFFFFFFFL
        val k = readInt().toLong() and 0xFFFFFFFFL
        return (v shl 32) + k
    }

    fun readUnsignedSmart(): Int {
        val peek = data[offset].toInt() and 0xFF
        return if (peek < 128) readUnsignedByte() else readUnsignedShort() - 32768
    }

    fun readUnsignedSmartMin1(): Int = readUnsignedSmart() - 1

    fun readBigSmart(): Int {
        if (data[offset] < 0) {
            return readInt() and 0x7FFFFFFF
        }
        val value = readUnsignedShort()
        return if (value == 32767) -1 else value
    }

    fun readBytes(dest: ByteArray) {
        val length = dest.size
        System.arraycopy(data, offset, dest, 0, length)
        offset += length
    }

    /**
     * Same CP1252 / special-character map as [InputBuffer.readString].
     */
    fun readString(): String {
        val start = offset
        while (data[offset++].toInt() != 0) {
            /* empty */
        }
        val length = offset - start - 1
        if (length == 0) return ""
        val chars = CharArray(length)
        var n = 0
        for (i in 0 until length) {
            val ch = InputBuffer.byteToChar(data[start + i]) ?: continue
            chars[n++] = ch
        }
        return String(chars, 0, n)
    }
}

internal inline fun DecodeCursor.forEachOpcode(decode: DecodeCursor.(Int) -> Unit) {
    while (true) {
        val opcode = readUnsignedByte()
        if (opcode == 0) break
        decode(opcode)
    }
}
