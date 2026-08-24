package stan.qodat.cache.impl.oldschool.loader

import com.displee.io.impl.InputBuffer
import com.displee.io.impl.OutputBuffer
import stan.qodat.cache.impl.oldschool.definition.NpcConditionalOp
import stan.qodat.cache.impl.oldschool.definition.NpcConditionalSubOp
import stan.qodat.cache.impl.oldschool.definition.NpcDefinition
import stan.qodat.cache.impl.oldschool.definition.NpcSubOp

/**
 * OSRS npc (config archive 9) decoder/encoder.
 *
 * Short model ids (opcodes 1/60) and int model ids (61/62) coexist so a newer
 * decoder reads older payloads. Opcode 102 head-icon layout is gated by
 * [configureForRevision] (`rev >= 1493`). Unknown opcodes are ignored.
 */
class NpcLoader {

    var defaultHeadIconArchive = -1
    var rev210HeadIcons = true
    var rev233 = true

    fun configureForRevision(revision: Int): NpcLoader {
        rev210HeadIcons = revision >= REV_210_NPC_ARCHIVE_REV
        return this
    }

    fun load(id: Int, b: ByteArray): NpcDefinition {
        val def = NpcDefinition(id)
        InputBuffer(b).forEachOpcode { opcode -> def.decodeValues(opcode, this) }
        def.post()
        return def
    }

    fun encode(def: NpcDefinition, format: NpcEncodeFormat = NpcEncodeFormat.LATEST): ByteArray {
        val out = OutputBuffer(16)
        writeModels(out, def.models, format, shortOpcode = 1, intOpcode = 61)
        if (def.name != "null") {
            out.writeByte(2)
            out.writeString(def.name)
        }
        if (def.size != 1) {
            out.writeByte(12)
            out.writeByte(def.size)
        }
        writeAnim(out, 13, def.standingAnimation)
        writeAnim(out, 14, def.walkingAnimation)
        writeAnim(out, 15, def.idleRotateLeftAnimation)
        writeAnim(out, 16, def.idleRotateRightAnimation)
        if (def.rotate180Animation != -1 || def.rotateLeftAnimation != -1 || def.rotateRightAnimation != -1) {
            out.writeByte(17)
            out.writeShort(def.walkingAnimation.coerceAtLeast(0))
            out.writeShort(def.rotate180Animation.coerceAtLeast(0))
            out.writeShort(def.rotateLeftAnimation.coerceAtLeast(0))
            out.writeShort(def.rotateRightAnimation.coerceAtLeast(0))
        }
        if (def.category != 0) {
            out.writeByte(18)
            out.writeShort(def.category)
        }
        for (i in def.actions.indices) {
            val action = def.actions[i] ?: continue
            out.writeByte(30 + i)
            out.writeString(action)
        }
        writePairs(out, 40, def.recolorToFind, def.recolorToReplace)
        writePairs(out, 41, def.retextureToFind, def.retextureToReplace)
        writeModels(out, def.chatheadModels, format, shortOpcode = 60, intOpcode = 62)
        for (i in def.stats.indices) {
            if (def.stats[i] != 1) {
                out.writeByte(74 + i)
                out.writeShort(def.stats[i])
            }
        }
        if (!def.isMinimapVisible) out.writeByte(93)
        if (def.combatLevel != -1) {
            out.writeByte(95)
            out.writeShort(def.combatLevel)
        }
        if (def.widthScale != 128) {
            out.writeByte(97)
            out.writeShort(def.widthScale)
        }
        if (def.heightScale != 128) {
            out.writeByte(98)
            out.writeShort(def.heightScale)
        }
        if (def.renderPriority == 1) out.writeByte(99)
        if (def.ambient != 0) {
            out.writeByte(100)
            out.writeByte(def.ambient)
        }
        if (def.contrast != 0) {
            out.writeByte(101)
            out.writeByte(def.contrast)
        }
        writeHeadIcons(out, def, format)
        if (def.rotationSpeed != 32) {
            out.writeByte(103)
            out.writeShort(def.rotationSpeed)
        }
        writeConfigs(out, def)
        if (!def.isInteractable) out.writeByte(107)
        if (!def.rotationFlag) out.writeByte(109)
        if (def.renderPriority == 2) out.writeByte(111)
        writeAnim(out, 114, def.runAnimation)
        if (def.runRotate180Animation != -1 || def.runRotateLeftAnimation != -1 || def.runRotateRightAnimation != -1) {
            out.writeByte(115)
            out.writeShort(def.runAnimation.coerceAtLeast(0))
            out.writeShort(def.runRotate180Animation.coerceAtLeast(0))
            out.writeShort(def.runRotateLeftAnimation.coerceAtLeast(0))
            out.writeShort(def.runRotateRightAnimation.coerceAtLeast(0))
        }
        writeAnim(out, 116, def.crawlAnimation)
        if (def.crawlRotate180Animation != -1 || def.crawlRotateLeftAnimation != -1 || def.crawlRotateRightAnimation != -1) {
            out.writeByte(117)
            out.writeShort(def.crawlAnimation.coerceAtLeast(0))
            out.writeShort(def.crawlRotate180Animation.coerceAtLeast(0))
            out.writeShort(def.crawlRotateLeftAnimation.coerceAtLeast(0))
            out.writeShort(def.crawlRotateRightAnimation.coerceAtLeast(0))
        }
        if (def.isFollower) out.writeByte(122)
        if (def.lowPriorityFollowerOps) out.writeByte(123)
        if (def.height != -1) {
            out.writeByte(124)
            out.writeShort(def.height)
        }
        val defaultFootprint = defaultFootprintSize(def.size)
        if (def.footprintSize != defaultFootprint) {
            out.writeByte(126)
            out.writeShort(def.footprintSize)
        }
        if (def.unknown1) out.writeByte(129)
        if (def.idleAnimRestart) out.writeByte(130)
        if (def.canHideForOverlap) out.writeByte(145)
        if (def.overlapTintHSL != 39188) {
            out.writeByte(146)
            out.writeShort(def.overlapTintHSL)
        }
        if (!def.zbuf) out.writeByte(147)
        writeParams(out, def.params)
        if (format == NpcEncodeFormat.LATEST) {
            for (op in def.subOps) {
                out.writeByte(251)
                out.writeByte(op.index)
                out.writeByte(op.subId)
                out.writeString(op.text)
            }
            for (op in def.conditionalOps) {
                out.writeByte(252)
                out.writeByte(op.index)
                out.writeShort(op.varpId)
                out.writeShort(op.varbitId)
                out.writeInt(op.minValue)
                out.writeInt(op.maxValue)
                out.writeString(op.text)
            }
            for (op in def.conditionalSubOps) {
                out.writeByte(253)
                out.writeByte(op.index)
                out.writeShort(op.subId)
                out.writeShort(op.varpId)
                out.writeShort(op.varbitId)
                out.writeInt(op.minValue)
                out.writeInt(op.maxValue)
                out.writeString(op.text)
            }
        }
        out.writeByte(0)
        return out.array()
    }

    private fun NpcDefinition.decodeValues(opcode: Int, stream: InputBuffer) {
        when (opcode) {
            1 -> models = stream.readShortIdArray()
            2 -> name = stream.readString()
            12 -> size = stream.readUnsignedByte()
            13 -> standingAnimation = stream.readUnsignedShort()
            14 -> walkingAnimation = stream.readUnsignedShort()
            15 -> idleRotateLeftAnimation = stream.readUnsignedShort()
            16 -> idleRotateRightAnimation = stream.readUnsignedShort()
            17 -> {
                walkingAnimation = stream.readUnsignedShort()
                rotate180Animation = stream.readUnsignedShort()
                rotateLeftAnimation = stream.readUnsignedShort()
                rotateRightAnimation = stream.readUnsignedShort()
            }
            18 -> category = stream.readUnsignedShort()
            in 30..34 -> {
                val text = stream.readString()
                if (!text.equals("Hidden", ignoreCase = true)) {
                    actions[opcode - 30] = text
                }
            }
            40 -> {
                val length = stream.readUnsignedByte()
                recolorToFind = ShortArray(length)
                recolorToReplace = ShortArray(length)
                repeat(length) {
                    recolorToFind!![it] = stream.readUnsignedShort().toShort()
                    recolorToReplace!![it] = stream.readUnsignedShort().toShort()
                }
            }
            41 -> {
                val length = stream.readUnsignedByte()
                retextureToFind = ShortArray(length)
                retextureToReplace = ShortArray(length)
                repeat(length) {
                    retextureToFind!![it] = stream.readUnsignedShort().toShort()
                    retextureToReplace!![it] = stream.readUnsignedShort().toShort()
                }
            }
            60 -> chatheadModels = stream.readShortIdArray()
            61 -> models = stream.readIntIdArray()
            62 -> chatheadModels = stream.readIntIdArray()
            74 -> stats[0] = stream.readUnsignedShort()
            75 -> stats[1] = stream.readUnsignedShort()
            76 -> stats[2] = stream.readUnsignedShort()
            77 -> stats[3] = stream.readUnsignedShort()
            78 -> stats[4] = stream.readUnsignedShort()
            79 -> stats[5] = stream.readUnsignedShort()
            93 -> isMinimapVisible = false
            95 -> combatLevel = stream.readUnsignedShort()
            97 -> widthScale = stream.readUnsignedShort()
            98 -> heightScale = stream.readUnsignedShort()
            99 -> renderPriority = 1
            100 -> ambient = stream.readByte().toInt()
            101 -> contrast = stream.readByte().toInt()
            102 -> decodeHeadIcons(stream)
            103 -> rotationSpeed = stream.readUnsignedShort()
            106 -> decodeConfigs(stream, defaultConfig = -1)
            107 -> isInteractable = false
            109 -> rotationFlag = false
            111 -> if (!rev233) {
                isFollower = true
                lowPriorityFollowerOps = true
            } else {
                renderPriority = 2
            }
            114 -> runAnimation = stream.readUnsignedShort()
            115 -> {
                runAnimation = stream.readUnsignedShort()
                runRotate180Animation = stream.readUnsignedShort()
                runRotateLeftAnimation = stream.readUnsignedShort()
                runRotateRightAnimation = stream.readUnsignedShort()
            }
            116 -> crawlAnimation = stream.readUnsignedShort()
            117 -> {
                crawlAnimation = stream.readUnsignedShort()
                crawlRotate180Animation = stream.readUnsignedShort()
                crawlRotateLeftAnimation = stream.readUnsignedShort()
                crawlRotateRightAnimation = stream.readUnsignedShort()
            }
            118 -> {
                varbitId = stream.readConfigId()
                varpIndex = stream.readConfigId()
                decodeConfigChildren(stream, stream.readConfigId())
            }
            122 -> isFollower = true
            123 -> lowPriorityFollowerOps = true
            124 -> height = stream.readUnsignedShort()
            126 -> footprintSize = stream.readUnsignedShort()
            129 -> unknown1 = true
            130 -> idleAnimRestart = true
            145 -> canHideForOverlap = true
            146 -> overlapTintHSL = stream.readUnsignedShort()
            147 -> zbuf = false
            249 -> params = stream.readNpcParams()
            251 -> subOps += NpcSubOp(
                index = stream.readUnsignedByte(),
                subId = stream.readUnsignedByte(),
                text = stream.readString(),
            )
            252 -> conditionalOps += NpcConditionalOp(
                index = stream.readUnsignedByte(),
                varpId = stream.readUnsignedShort(),
                varbitId = stream.readUnsignedShort(),
                minValue = stream.readInt(),
                maxValue = stream.readInt(),
                text = stream.readString(),
            )
            253 -> conditionalSubOps += NpcConditionalSubOp(
                index = stream.readUnsignedByte(),
                subId = stream.readUnsignedShort(),
                varpId = stream.readUnsignedShort(),
                varbitId = stream.readUnsignedShort(),
                minValue = stream.readInt(),
                maxValue = stream.readInt(),
                text = stream.readString(),
            )
            else -> Unit
        }
    }

    private fun NpcDefinition.decodeHeadIcons(stream: InputBuffer) {
        if (!rev210HeadIcons) {
            headIconArchiveIds = intArrayOf(defaultHeadIconArchive)
            headIconSpriteIndex = shortArrayOf(stream.readUnsignedShort().toShort())
            return
        }
        val bitfield = stream.readUnsignedByte()
        var len = 0
        var bits = bitfield
        while (bits != 0) {
            ++len
            bits = bits shr 1
        }
        val archives = IntArray(len)
        val sprites = ShortArray(len)
        for (i in 0 until len) {
            if (bitfield and (1 shl i) == 0) {
                archives[i] = -1
                sprites[i] = -1
            } else {
                archives[i] = stream.readBigSmart()
                sprites[i] = stream.readUnsignedSmartMin1().toShort()
            }
        }
        headIconArchiveIds = archives
        headIconSpriteIndex = sprites
    }

    private fun NpcDefinition.decodeConfigs(stream: InputBuffer, defaultConfig: Int) {
        varbitId = stream.readConfigId()
        varpIndex = stream.readConfigId()
        decodeConfigChildren(stream, defaultConfig)
    }

    private fun NpcDefinition.decodeConfigChildren(stream: InputBuffer, defaultConfig: Int) {
        val length = stream.readUnsignedByte()
        val values = IntArray(length + 2)
        for (index in 0..length) {
            values[index] = stream.readConfigId()
        }
        values[length + 1] = defaultConfig
        configs = values
    }

    private fun NpcDefinition.post() {
        if (footprintSize == -1) {
            footprintSize = defaultFootprintSize(size)
        }
    }
}

enum class NpcEncodeFormat {
    /** Oldest interesting payload: short model/chathead ids, pre-210 opcode 102. */
    SHORT_MODEL,
    /** Current client table: int model ids, rev-210 head icons, extra ops. */
    LATEST,
}

const val REV_210_NPC_ARCHIVE_REV = 1493

private fun defaultFootprintSize(size: Int): Int = (0.4F * (size * 128).toFloat()).toInt()

private fun InputBuffer.readShortIdArray(): IntArray {
    val length = readUnsignedByte()
    return IntArray(length) { readUnsignedShort() }
}

private fun InputBuffer.readIntIdArray(): IntArray {
    val length = readUnsignedByte()
    return IntArray(length) { readInt() }
}

private fun InputBuffer.readConfigId(): Int {
    val value = readUnsignedShort()
    return if (value == 0xFFFF) -1 else value
}

private fun InputBuffer.readNpcParams(): HashMap<Int, Any> {
    val length = readUnsignedByte()
    val params = HashMap<Int, Any>(length)
    repeat(length) {
        val type = readUnsignedByte()
        val key = read24BitInt()
        val value: Any = when (type) {
            1 -> readString()
            2 -> readLong()
            else -> readInt()
        }
        params[key] = value
    }
    return params
}

private fun writeModels(
    out: OutputBuffer,
    models: IntArray?,
    format: NpcEncodeFormat,
    shortOpcode: Int,
    intOpcode: Int,
) {
    if (models == null || models.isEmpty()) return
    when (format) {
        NpcEncodeFormat.SHORT_MODEL -> {
            require(models.all { it in 0..0xFFFF }) { "model id does not fit opcode $shortOpcode" }
            out.writeByte(shortOpcode)
            out.writeByte(models.size)
            models.forEach { out.writeShort(it) }
        }
        NpcEncodeFormat.LATEST -> {
            out.writeByte(intOpcode)
            out.writeByte(models.size)
            models.forEach { out.writeInt(it) }
        }
    }
}

private fun writeAnim(out: OutputBuffer, opcode: Int, id: Int) {
    if (id == -1) return
    out.writeByte(opcode)
    out.writeShort(id)
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

private fun writeHeadIcons(out: OutputBuffer, def: NpcDefinition, format: NpcEncodeFormat) {
    val archives = def.headIconArchiveIds ?: return
    val sprites = def.headIconSpriteIndex ?: return
    if (archives.isEmpty()) return
    out.writeByte(102)
    when (format) {
        NpcEncodeFormat.SHORT_MODEL -> {
            out.writeShort(sprites[0].toInt() and 0xFFFF)
        }
        NpcEncodeFormat.LATEST -> {
            var bitfield = 0
            for (i in archives.indices) {
                if (archives[i] != -1 || sprites[i].toInt() != -1) {
                    bitfield = bitfield or (1 shl i)
                }
            }
            out.writeByte(bitfield)
            for (i in archives.indices) {
                if (bitfield and (1 shl i) == 0) continue
                out.writeBigSmart(archives[i])
                writeUnsignedShortSmartMinusOne(out, sprites[i].toInt())
            }
        }
    }
}

private fun writeUnsignedShortSmartMinusOne(out: OutputBuffer, value: Int) {
    val encoded = value + 1
    if (encoded in 0 until 128) {
        out.writeByte(encoded)
    } else {
        out.writeShort(value + 0x8001)
    }
}

private fun writeConfigs(out: OutputBuffer, def: NpcDefinition) {
    val configs = def.configs ?: return
    if (configs.isEmpty()) return
    val defaultConfig = configs.last()
    val childCount = configs.size - 2
    val opcode = if (defaultConfig == -1) 106 else 118
    out.writeByte(opcode)
    out.writeShort(if (def.varbitId == -1) 0xFFFF else def.varbitId)
    out.writeShort(if (def.varpIndex == -1) 0xFFFF else def.varpIndex)
    if (opcode == 118) {
        out.writeShort(if (defaultConfig == -1) 0xFFFF else defaultConfig)
    }
    out.writeByte(childCount)
    repeat(childCount + 1) { index ->
        val value = configs[index]
        out.writeShort(if (value == -1) 0xFFFF else value)
    }
}

private fun writeParams(out: OutputBuffer, params: Map<Int, Any>?) {
    if (params.isNullOrEmpty()) return
    out.writeByte(249)
    out.writeByte(params.size)
    for ((key, value) in params) {
        when (value) {
            is String -> {
                out.writeByte(1)
                out.write24BitInt(key)
                out.writeString(value)
            }
            is Long -> {
                out.writeByte(2)
                out.write24BitInt(key)
                out.writeLong(value)
            }
            is Number -> {
                out.writeByte(0)
                out.write24BitInt(key)
                out.writeInt(value.toInt())
            }
        }
    }
}
