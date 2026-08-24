package stan.qodat.cache.impl.oldschool.loader

import net.runelite.cache.io.InputStream
import stan.qodat.cache.impl.oldschool.definition.SequenceCommonFields

internal inline fun InputStream.forEachOpcode(decode: InputStream.(Int) -> Unit) {
    while (true) {
        val opcode = readUnsignedByte()
        if (opcode == 0) break
        decode(opcode)
    }
}

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

internal fun InputStream.readInterleaveLeave(): IntArray {
    val length = readUnsignedByte()
    return IntArray(1 + length) {
        if (it == length) 9999999 else readUnsignedByte()
    }
}

internal fun SequenceCommonFields.applySharedSequenceOpcode(opcode: Int, stream: InputStream): Boolean {
    when (opcode) {
        1 -> {
            val (lengths, ids) = stream.readFrameLengthAndIdTables()
            frameLenghts = lengths
            frameIDs = ids
        }
        2 -> frameStep = stream.readUnsignedShort()
        3 -> interleaveLeave = stream.readInterleaveLeave()
        4 -> stretches = true
        5 -> forcedPriority = stream.readUnsignedByte()
        6 -> leftHandItem = stream.readUnsignedShort()
        7 -> rightHandItem = stream.readUnsignedShort()
        8 -> maxLoops = stream.readUnsignedByte()
        9 -> precedenceAnimating = stream.readUnsignedByte()
        10 -> priority = stream.readUnsignedByte()
        11 -> replyMode = stream.readUnsignedByte()
        12 -> chatFrameIds = stream.readPackedArchiveFileIds()
        18 -> name = stream.readString()
        else -> return false
    }
    return true
}
