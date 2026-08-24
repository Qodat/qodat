package stan.qodat.cache.impl.oldschool.loader

import net.runelite.cache.io.InputStream

internal fun InputStream.readFrameLengthAndIdTables(): Pair<IntArray, IntArray> {
    val length = readUnsignedShort()
    val frameLengths = IntArray(length) { readUnsignedShort() }
    val frameIds = IntArray(length) { readUnsignedShort() }
    for (i in frameIds.indices) {
        frameIds[i] += readUnsignedShort() shl 16
    }
    return frameLengths to frameIds
}

internal fun InputStream.readPackedArchiveFileIds(): IntArray {
    val length = readUnsignedByte()
    val ids = IntArray(length) { readUnsignedShort() }
    for (i in ids.indices) {
        ids[i] += readUnsignedShort() shl 16
    }
    return ids
}
