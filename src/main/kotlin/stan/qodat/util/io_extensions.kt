package stan.qodat.util

import net.runelite.cache.io.OutputStream

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   2019-08-18
 * @version 1.0
 */

fun OutputStream.writeByteOrShort(value: Int) {
    when {
        value >= -0x40 && value < 0x40 -> writeByte(value + 64)
        value >= -0x8000 && value < 0x8000 -> writeShort(value + 49152)
        else -> throw IllegalArgumentException()
    }
}

