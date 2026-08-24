package stan.qodat.cache.impl.oldschool.loader

import com.displee.io.impl.InputBuffer
import qodat.cache.definition.AnimationSound
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
        InputBuffer(b).forEachOpcode { opcode -> def.decodeValues(opcode, this) }
        return def
    }

    fun configureForRevision(revision: Int) {
        this.rev220FrameSounds = revision > REV_220_SEQ_ARCHIVE_REV
        this.rev226 = revision > REV_226_SEQ_ARCHIVE_REV
    }

    private fun SequenceDefinition226.decodeValues(opcode: Int, stream: InputBuffer) {
        if (applySharedSequenceOpcode(opcode, stream)) return
        when (opcode) {
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
                } else {
                    verticalOffset = stream.readByte().toInt()
                }
            }

            17 -> {
                animMayaMasks = BooleanArray(256) { false }
                repeat(stream.readUnsignedByte()) {
                    val index = stream.readUnsignedByte()
                    animMayaMasks?.set(index, true)
                }
            }

            19 -> soundsCrossWorldView = true
            else -> Unit
        }
    }

    private fun readFrameSound(stream: InputBuffer): AnimationSound? {
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
            AnimationSound(id, loops, location, retain, weight)
        else
            null
    }
}

private const val REV_220_SEQ_ARCHIVE_REV = 1141
private const val REV_226_SEQ_ARCHIVE_REV = 1268
