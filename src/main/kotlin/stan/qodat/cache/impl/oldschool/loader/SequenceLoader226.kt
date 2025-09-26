package stan.qodat.cache.impl.oldschool.loader

import net.runelite.cache.definitions.SequenceDefinition
import net.runelite.cache.io.InputStream
import stan.qodat.cache.impl.oldschool.definition.SequenceDefinition206
import stan.qodat.cache.impl.oldschool.definition.SequenceDefinition226

/**
 * Updated loader based of RuneLite's [net.runelite.cache.definitions.loaders.SequenceLoader].
 *
 * @author Stan van der Bend
 */
class SequenceLoader226 {

    private var rev220FrameSounds = false
    private var rev226 = false

    fun load(id: Int, b: ByteArray): SequenceDefinition226 {
        val def = SequenceDefinition226(id.toString())
        val `is` = InputStream(b)
        while (true) {
            val opcode = `is`.readUnsignedByte()
            if (opcode == 0) {
                break
            }
//            println("decoding[$id]: Found opcode $opcode")
            def.decodeValues(opcode, `is`)
        }
        return def
    }

    fun configureForRevision(revision: Int) {
        this.rev220FrameSounds = revision > 1141;
        this.rev226 = revision > 1268;
    }

    private fun SequenceDefinition226.decodeValues(opcode: Int, stream: InputStream) {
        val length: Int
        var i: Int
        when (opcode) {
            1 -> {
                length = stream.readUnsignedShort()
                frameLenghts = IntArray(length) {
                    stream.readUnsignedShort()
                }
                frameIDs = IntArray(length) {
                    stream.readUnsignedShort()
                }
                i = 0
                while (i < length) {
                    frameIDs!![i] += stream.readUnsignedShort() shl 16
                    ++i
                }
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
            12 -> {
                length = stream.readUnsignedByte()
                chatFrameIds = IntArray(length) {
                    stream.readUnsignedShort()
                }
                i = 0
                while (i < length) {
                    chatFrameIds!![i] += stream.readUnsignedShort() shl 16
                    ++i
                }
            }
            13 -> {
                if (rev226) {
                    animMayaId = stream.readInt()
                } else {
                    sounds = buildMap {
                        repeat(stream.readUnsignedByte()) {
                            put(it, readFrameSound(stream))
                        }
                    }
                }
            }
            14 -> {
                if (rev226) {
                    sounds = buildMap {
                        repeat(stream.readUnsignedShort()) {
                            val frame = stream.readUnsignedShort()
                            put(frame, readFrameSound(stream))
                        }
                    }
                } else {
                    animMayaId = stream.readInt()
                }
            }
            15 -> {
                if (rev226) {
                    animMayaStart = stream.readUnsignedShort()
                    animMayaEnd = stream.readUnsignedShort()
                } else {
                    sounds = buildMap {
                        repeat(stream.readUnsignedShort()) {
                            val frame = stream.readUnsignedShort()
                            put(frame, readFrameSound(stream))
                        }
                    }
                }
            }
            16 -> {
                if (!rev226) {
                    animMayaStart = stream.readUnsignedShort()
                    animMayaEnd = stream.readUnsignedShort()
                }
            }
            17 -> {
                animMayaMasks = BooleanArray(256) { false }
                repeat(stream.readUnsignedByte()) {
                    val index = stream.readUnsignedByte()
                    animMayaMasks?.set(index, true)
                }
            }
            18 -> {
                name = stream.readString()
            }
        }
    }

    private fun readFrameSound(stream: InputStream): Sound? {
        val location: Int
        var weight: Int = -1
        val loops: Int
        val retain: Int
        val id: Int
        if (!rev220FrameSounds) {
            val bits = stream.read24BitInt()
            location = bits and 15
            id = bits shr 8
            loops = (bits shr 4) and 7
            retain = 0
        } else {
            id = stream.readUnsignedShort()
            if (rev226) {
                weight = stream.readUnsignedByte()
            }
            loops = stream.readUnsignedByte()
            location = stream.readUnsignedByte()
            retain = stream.readUnsignedByte()
        }
        return if (id >= 1 && loops >= 1 && location >= 0 && retain >= 0)
            Sound(id, location, weight, loops, retain)
        else
            null
    }
}

class Sound(val id: Int, val location: Int, val weight: Int, val loops: Int, val retain: Int) {

    fun toRuneliteSound() =
        SequenceDefinition.Sound(id, loops, location, retain)
}