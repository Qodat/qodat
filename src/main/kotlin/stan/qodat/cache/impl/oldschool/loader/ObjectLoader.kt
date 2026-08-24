package stan.qodat.cache.impl.oldschool.loader

import com.displee.io.impl.InputBuffer
import com.displee.io.impl.OutputBuffer
import stan.qodat.cache.impl.oldschool.definition.ObjectConditionalOp
import stan.qodat.cache.impl.oldschool.definition.ObjectConditionalSubOp
import stan.qodat.cache.impl.oldschool.definition.ObjectDefinition
import stan.qodat.cache.impl.oldschool.definition.ObjectSubOp

/**
 * OSRS loc/object (config archive 6) decoder/encoder.
 *
 * Short model ids (opcodes 1/5) and int model ids (6/7) coexist so a newer
 * decoder reads older payloads. Opcode 78/79 ambient-sound retain is gated by
 * [configureForRevision] (`rev >= 1673`). Unknown opcodes are ignored.
 */
class ObjectLoader {

    var rev220SoundData = true

    fun configureForRevision(revision: Int): ObjectLoader {
        rev220SoundData = revision >= REV_220_OBJ_ARCHIVE_REV
        return this
    }

    fun load(id: Int, b: ByteArray): ObjectDefinition {
        val def = ObjectDefinition(id)
        InputBuffer(b).forEachOpcode { opcode -> def.decodeValues(opcode, this) }
        def.post()
        return def
    }

    fun encode(def: ObjectDefinition, format: ObjectEncodeFormat = ObjectEncodeFormat.LATEST): ByteArray {
        val out = OutputBuffer(16)
        writeModels(out, def, format)
        if (def.name != "null") {
            out.writeByte(2)
            out.writeString(def.name)
        }
        if (def.sizeX != 1) {
            out.writeByte(14)
            out.writeByte(def.sizeX)
        }
        if (def.sizeY != 1) {
            out.writeByte(15)
            out.writeByte(def.sizeY)
        }
        if (def.interactType == 0) {
            out.writeByte(17)
        } else {
            if (def.interactType == 1) out.writeByte(27)
            if (!def.blocksProjectile) out.writeByte(18)
        }
        if (def.wallOrDoor != -1) {
            out.writeByte(19)
            out.writeByte(def.wallOrDoor)
        }
        if (def.contouredGround == 0) {
            out.writeByte(21)
        } else if (def.contouredGround != -1) {
            out.writeByte(81)
            out.writeByte(def.contouredGround / 256)
        }
        if (def.mergeNormals) out.writeByte(22)
        if (def.modelClipped) out.writeByte(23)
        if (def.animationId != -1) {
            out.writeByte(24)
            out.writeShort(def.animationId)
        }
        if (def.decorDisplacement != 16) {
            out.writeByte(28)
            out.writeByte(def.decorDisplacement)
        }
        if (def.ambient != 0) {
            out.writeByte(29)
            out.writeByte(def.ambient)
        }
        if (def.contrast != 0) {
            out.writeByte(39)
            out.writeByte(def.contrast / 25)
        }
        for (i in def.actions.indices) {
            val action = def.actions[i] ?: continue
            out.writeByte(30 + i)
            out.writeString(action)
        }
        writePairs(out, 40, def.recolorToFind, def.recolorToReplace)
        writePairs(out, 41, def.retextureToFind, def.textureToReplace)
        if (def.category != 0) {
            out.writeByte(61)
            out.writeShort(def.category)
        }
        if (def.isRotated) out.writeByte(62)
        if (!def.shadow) out.writeByte(64)
        if (def.modelSizeX != 128) {
            out.writeByte(65)
            out.writeShort(def.modelSizeX)
        }
        if (def.modelSizeHeight != 128) {
            out.writeByte(66)
            out.writeShort(def.modelSizeHeight)
        }
        if (def.modelSizeY != 128) {
            out.writeByte(67)
            out.writeShort(def.modelSizeY)
        }
        if (def.mapSceneID != -1) {
            out.writeByte(68)
            out.writeShort(def.mapSceneID)
        }
        if (def.blockingMask != 0) {
            out.writeByte(69)
            out.writeByte(def.blockingMask)
        }
        if (def.offsetX != 0) {
            out.writeByte(70)
            out.writeShort(def.offsetX)
        }
        if (def.offsetHeight != 0) {
            out.writeByte(71)
            out.writeShort(def.offsetHeight)
        }
        if (def.offsetY != 0) {
            out.writeByte(72)
            out.writeShort(def.offsetY)
        }
        if (def.obstructsGround) out.writeByte(73)
        if (def.isHollow) out.writeByte(74)
        if (def.supportsItems != -1) {
            out.writeByte(75)
            out.writeByte(def.supportsItems)
        }
        writeConfigs(out, def)
        writeAmbientSound(out, def, format)
        if (def.mapAreaId != -1) {
            out.writeByte(82)
            out.writeShort(def.mapAreaId)
        }
        if (def.randomizeAnimStart) out.writeByte(89)
        if (def.deferAnimChange) out.writeByte(90)
        if (def.soundDistanceFadeCurve != 0) {
            out.writeByte(91)
            out.writeByte(def.soundDistanceFadeCurve)
        }
        if (def.soundFadeInCurve != 0 || def.soundFadeInDuration != 300 ||
            def.soundFadeOutCurve != 0 || def.soundFadeOutDuration != 300
        ) {
            out.writeByte(93)
            out.writeByte(def.soundFadeInCurve)
            out.writeShort(def.soundFadeInDuration)
            out.writeByte(def.soundFadeOutCurve)
            out.writeShort(def.soundFadeOutDuration)
        }
        if (def.unknown1) out.writeByte(94)
        if (def.soundVisibility != 2) {
            out.writeByte(95)
            out.writeByte(def.soundVisibility)
        }
        if (def.raise != 0) {
            out.writeByte(96)
            out.writeByte(def.raise)
        }
        if (format == ObjectEncodeFormat.LATEST) {
            for (op in def.subOps) {
                out.writeByte(100)
                out.writeByte(op.index)
                out.writeByte(op.subId)
                out.writeString(op.text)
            }
            for (op in def.conditionalOps) {
                out.writeByte(101)
                out.writeByte(op.index)
                out.writeShort(op.varpId)
                out.writeShort(op.varbitId)
                out.writeInt(op.minValue)
                out.writeInt(op.maxValue)
                out.writeString(op.text)
            }
            for (op in def.conditionalSubOps) {
                out.writeByte(102)
                out.writeByte(op.index)
                out.writeShort(op.subId)
                out.writeShort(op.varpId)
                out.writeShort(op.varbitId)
                out.writeInt(op.minValue)
                out.writeInt(op.maxValue)
                out.writeString(op.text)
            }
        }
        writeParams(out, def.params)
        out.writeByte(0)
        return out.array()
    }

    private fun ObjectDefinition.decodeValues(opcode: Int, stream: InputBuffer) {
        when (opcode) {
            1 -> decodeModels(stream, intIds = false, withTypes = true)
            2 -> name = stream.readString()
            5 -> decodeModels(stream, intIds = false, withTypes = false)
            6 -> decodeModels(stream, intIds = true, withTypes = true)
            7 -> decodeModels(stream, intIds = true, withTypes = false)
            14 -> sizeX = stream.readUnsignedByte()
            15 -> sizeY = stream.readUnsignedByte()
            17 -> {
                interactType = 0
                blocksProjectile = false
            }
            18 -> blocksProjectile = false
            19 -> wallOrDoor = stream.readUnsignedByte()
            21 -> contouredGround = 0
            22 -> mergeNormals = true
            23 -> modelClipped = true
            24 -> animationId = stream.readConfigId()
            27 -> interactType = 1
            28 -> decorDisplacement = stream.readUnsignedByte()
            29 -> ambient = stream.readByte().toInt()
            39 -> contrast = stream.readByte().toInt() * 25
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
                textureToReplace = ShortArray(length)
                repeat(length) {
                    retextureToFind!![it] = stream.readUnsignedShort().toShort()
                    textureToReplace!![it] = stream.readUnsignedShort().toShort()
                }
            }
            61 -> category = stream.readUnsignedShort()
            62 -> isRotated = true
            64 -> shadow = false
            65 -> modelSizeX = stream.readUnsignedShort()
            66 -> modelSizeHeight = stream.readUnsignedShort()
            67 -> modelSizeY = stream.readUnsignedShort()
            68 -> mapSceneID = stream.readUnsignedShort()
            69 -> blockingMask = stream.readByte().toInt()
            70 -> offsetX = stream.readUnsignedShort()
            71 -> offsetHeight = stream.readUnsignedShort()
            72 -> offsetY = stream.readUnsignedShort()
            73 -> obstructsGround = true
            74 -> isHollow = true
            75 -> supportsItems = stream.readUnsignedByte()
            77 -> decodeConfigs(stream, defaultConfig = -1)
            78 -> {
                ambientSoundId = stream.readUnsignedShort()
                ambientSoundDistance = stream.readUnsignedByte()
                if (rev220SoundData) ambientSoundRetain = stream.readUnsignedByte()
            }
            79 -> {
                ambientSoundChangeTicksMin = stream.readUnsignedShort()
                ambientSoundChangeTicksMax = stream.readUnsignedShort()
                ambientSoundDistance = stream.readUnsignedByte()
                if (rev220SoundData) ambientSoundRetain = stream.readUnsignedByte()
                val length = stream.readUnsignedByte()
                ambientSoundIds = IntArray(length) { stream.readUnsignedShort() }
            }
            81 -> contouredGround = stream.readUnsignedByte() * 256
            82 -> mapAreaId = stream.readUnsignedShort()
            89 -> randomizeAnimStart = true
            90 -> deferAnimChange = true
            91 -> soundDistanceFadeCurve = stream.readUnsignedByte()
            92 -> {
                varbitId = stream.readConfigId()
                varpId = stream.readConfigId()
                decodeConfigChildren(stream, stream.readConfigId())
            }
            93 -> {
                soundFadeInCurve = stream.readUnsignedByte()
                soundFadeInDuration = stream.readUnsignedShort()
                soundFadeOutCurve = stream.readUnsignedByte()
                soundFadeOutDuration = stream.readUnsignedShort()
            }
            94 -> unknown1 = true
            95 -> soundVisibility = stream.readUnsignedByte()
            96 -> raise = stream.readUnsignedByte()
            100 -> subOps += ObjectSubOp(
                index = stream.readUnsignedByte(),
                subId = stream.readUnsignedByte(),
                text = stream.readString(),
            )
            101 -> conditionalOps += ObjectConditionalOp(
                index = stream.readUnsignedByte(),
                varpId = stream.readUnsignedShort(),
                varbitId = stream.readUnsignedShort(),
                minValue = stream.readInt(),
                maxValue = stream.readInt(),
                text = stream.readString(),
            )
            102 -> conditionalSubOps += ObjectConditionalSubOp(
                index = stream.readUnsignedByte(),
                subId = stream.readUnsignedShort(),
                varpId = stream.readUnsignedShort(),
                varbitId = stream.readUnsignedShort(),
                minValue = stream.readInt(),
                maxValue = stream.readInt(),
                text = stream.readString(),
            )
            249 -> params = stream.readObjectParams()
            else -> Unit
        }
    }

    private fun ObjectDefinition.decodeModels(stream: InputBuffer, intIds: Boolean, withTypes: Boolean) {
        val length = stream.readUnsignedByte()
        if (length <= 0) return
        if (!withTypes) objectTypes = null
        val models = IntArray(length)
        val types = if (withTypes) IntArray(length) else null
        repeat(length) { index ->
            models[index] = if (intIds) stream.readInt() else stream.readUnsignedShort()
            if (types != null) types[index] = stream.readUnsignedByte()
        }
        objectModels = models
        if (types != null) objectTypes = types
    }

    private fun ObjectDefinition.decodeConfigs(stream: InputBuffer, defaultConfig: Int) {
        varbitId = stream.readConfigId()
        varpId = stream.readConfigId()
        decodeConfigChildren(stream, defaultConfig)
    }

    private fun ObjectDefinition.decodeConfigChildren(stream: InputBuffer, defaultConfig: Int) {
        val length = stream.readUnsignedByte()
        val values = IntArray(length + 2)
        for (index in 0..length) {
            values[index] = stream.readConfigId()
        }
        values[length + 1] = defaultConfig
        configChangeDest = values
    }

    private fun ObjectDefinition.post() {
        if (wallOrDoor == -1) {
            wallOrDoor = 0
            if (objectModels != null && (objectTypes == null || objectTypes!![0] == 10)) {
                wallOrDoor = 1
            }
            if (actions.any { it != null }) {
                wallOrDoor = 1
            }
        }
        if (supportsItems == -1) {
            supportsItems = if (interactType != 0) 1 else 0
        }
    }
}

enum class ObjectEncodeFormat {
    /** Oldest interesting payload: short model ids, pre-220 ambient sound. */
    SHORT_MODEL,
    /** Current client table: int model ids, rev-220 ambient-sound retain. */
    LATEST,
}

const val REV_220_OBJ_ARCHIVE_REV = 1673

private fun InputBuffer.readConfigId(): Int {
    val value = readUnsignedShort()
    return if (value == 0xFFFF) -1 else value
}

private fun InputBuffer.readObjectParams(): HashMap<Int, Any> {
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

private fun writeModels(out: OutputBuffer, def: ObjectDefinition, format: ObjectEncodeFormat) {
    val models = def.objectModels ?: return
    if (models.isEmpty()) return
    val types = def.objectTypes
    val intIds = format == ObjectEncodeFormat.LATEST
    if (!intIds) {
        require(models.all { it in 0..0xFFFF }) { "model id does not fit short object opcode" }
    }
    val opcode = when {
        types != null && intIds -> 6
        types != null -> 1
        intIds -> 7
        else -> 5
    }
    out.writeByte(opcode)
    out.writeByte(models.size)
    for (i in models.indices) {
        if (intIds) out.writeInt(models[i]) else out.writeShort(models[i])
        if (types != null) out.writeByte(types[i])
    }
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

private fun writeConfigs(out: OutputBuffer, def: ObjectDefinition) {
    val configs = def.configChangeDest ?: return
    if (configs.isEmpty()) return
    val defaultConfig = configs.last()
    val childCount = configs.size - 2
    val opcode = if (defaultConfig == -1) 77 else 92
    out.writeByte(opcode)
    out.writeShort(if (def.varbitId == -1) 0xFFFF else def.varbitId)
    out.writeShort(if (def.varpId == -1) 0xFFFF else def.varpId)
    if (opcode == 92) {
        out.writeShort(if (defaultConfig == -1) 0xFFFF else defaultConfig)
    }
    out.writeByte(childCount)
    repeat(childCount + 1) { index ->
        val value = configs[index]
        out.writeShort(if (value == -1) 0xFFFF else value)
    }
}

private fun writeAmbientSound(out: OutputBuffer, def: ObjectDefinition, format: ObjectEncodeFormat) {
    val writeRetain = format == ObjectEncodeFormat.LATEST
    if (def.ambientSoundId != -1) {
        out.writeByte(78)
        out.writeShort(def.ambientSoundId)
        out.writeByte(def.ambientSoundDistance)
        if (writeRetain) out.writeByte(def.ambientSoundRetain)
    }
    val ids = def.ambientSoundIds
    if (ids != null && ids.isNotEmpty()) {
        out.writeByte(79)
        out.writeShort(def.ambientSoundChangeTicksMin)
        out.writeShort(def.ambientSoundChangeTicksMax)
        out.writeByte(def.ambientSoundDistance)
        if (writeRetain) out.writeByte(def.ambientSoundRetain)
        out.writeByte(ids.size)
        ids.forEach { out.writeShort(it) }
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
