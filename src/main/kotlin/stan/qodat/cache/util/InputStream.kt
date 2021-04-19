/*
 * Copyright (c) 2016-2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package stan.qodat.cache.util

import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer

class InputStream(buffer: ByteArray) : InputStream() {

    private val buffer: ByteBuffer = ByteBuffer.wrap(buffer)

    val array: ByteArray
        get() {
            assert(buffer.hasArray())
            return buffer.array()
        }

    override fun toString(): String {
        return "InputStream{buffer=$buffer}"
    }

    fun read24BitInt(): Int {
        return (readUnsignedByte() shl 16) + (readUnsignedByte() shl 8) + readUnsignedByte()
    }

    fun skip(length: Int) {
        var pos = buffer.position()
        pos += length
        buffer.position(pos)
    }

    var offset: Int
        get() = buffer.position()
        set(offset) {
            buffer.position(offset)
        }
    val length: Int
        get() = buffer.limit()

    fun remaining(): Int {
        return buffer.remaining()
    }

    fun readByte(): Byte {
        return buffer.get()
    }

    fun readBytes(buffer: ByteArray?, off: Int, len: Int) {
        this.buffer[buffer, off, len]
    }

    fun readBytes(buffer: ByteArray?) {
        this.buffer[buffer]
    }

    fun readUnsignedByte(): Int {
        return readByte().toInt() and 0xFF
    }

    fun readUnsignedShort(): Int {
        return buffer.short.toInt() and 0xFFFF
    }

    fun readShort(): Short {
        return buffer.short
    }

    fun readInt(): Int {
        return buffer.int
    }

    fun peek(): Byte {
        return buffer[buffer.position()]
    }

    fun readBigSmart(): Int {
        return if (peek() >= 0) readUnsignedShort() and 0xFFFF else readInt() and Int.MAX_VALUE
    }

    fun readBigSmart2(): Int {
        if (peek() < 0) {
            return readInt() and Int.MAX_VALUE // and off sign bit
        }
        val value = readUnsignedShort()
        return if (value == 32767) -1 else value
    }

    /**
     * Reads a smart value from the buffer (supports -1).
     * @return the read smart value.
     */
    fun readSmartNS(): Int {
        return readUnsignedShortSmart() - 1
    }

    fun readShortSmart(): Int {
        val peek: Int = peek().toInt() and 0xFF
        return if (peek < 128) readUnsignedByte() - 64 else readUnsignedShort() - 0xc000
    }

    fun readUnsignedShortSmart(): Int {
        val peek: Int = peek().toInt() and 0xFF
        return if (peek < 128) readUnsignedByte() else readUnsignedShort() - 32768
    }

    fun readUnsignedIntSmartShortCompat(): Int {
        var var1 = 0
        var var2: Int
        var2 = readUnsignedShortSmart()
        while (var2 == 32767) {
            var1 += 32767
            var2 = readUnsignedShortSmart()
        }
        var1 += var2
        return var1
    }

    fun readString(): String {
        val sb = StringBuilder()
        while (true) {
            var ch = readUnsignedByte()
            if (ch == 0) {
                break
            }
            if (ch in 128..159) {
                var var7 = CHARACTERS[ch - 128]
                if (0 == var7.toInt()) {
                    var7 = '?'
                }
                ch = var7.toInt()
            }
            sb.append(ch.toChar())
        }
        return sb.toString()
    }

    fun readStringOrNull(): String? {
        return if (peek().toInt() != 0) {
            readString()
        } else {
            readByte() // discard
            null
        }
    }

    fun readVarInt(): Int {
        var var1 = readByte()
        var var2: Int
        var2 = 0
        while (var1 < 0) {
            var2 = var2 or var1.toInt() and 127 shl 7
            var1 = readByte()
        }
        return var2 or var1.toInt()
    }

    val remaining: ByteArray
        get() {
            val b = ByteArray(buffer.remaining())
            buffer[b]
            return b
        }

    @Throws(IOException::class)
    override fun read(): Int {
        return readUnsignedByte()
    }

    companion object {
        private val CHARACTERS = charArrayOf(
            '\u20ac', '\u0000', '\u201a', '\u0192', '\u201e', '\u2026',
            '\u2020', '\u2021', '\u02c6', '\u2030', '\u0160', '\u2039',
            '\u0152', '\u0000', '\u017d', '\u0000', '\u0000', '\u2018',
            '\u2019', '\u201c', '\u201d', '\u2022', '\u2013', '\u2014',
            '\u02dc', '\u2122', '\u0161', '\u203a', '\u0153', '\u0000',
            '\u017e', '\u0178'
        )
    }

}