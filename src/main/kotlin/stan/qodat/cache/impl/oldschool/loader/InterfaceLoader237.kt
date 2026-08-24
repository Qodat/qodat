package stan.qodat.cache.impl.oldschool.loader

import com.displee.io.impl.InputBuffer
import qodat.cache.definition.ClientScript1Instruction
import stan.qodat.cache.impl.oldschool.definition.InterfaceDefinition

/**
 * Interface decoder that understands Near Reality's rev237 IF3 prefix (`0xAABBCCDD`)
 * in addition to vanilla IF1 / IF3. Newer decode reads IF1, vanilla IF3, and 237.
 */
class InterfaceLoader237 {

    private var sourceSize = 0
    private var intModelIds = false

    fun configureForRevision(revision: Int): InterfaceLoader237 {
        intModelIds = revision > INT_MODEL_REVISION
        return this
    }

    fun load(id: Int, data: ByteArray): InterfaceDefinition {
        sourceSize = data.size
        val iface = InterfaceDefinition()
        iface.id = id

        val rev237 = hasRev237Magic(data)
        val offset = if (rev237) MAGIC_SIZE else 0
        val stream = InputBuffer(data)
        stream.offset = offset
        if (data[offset] == (-1).toByte()) {
            decodeIf3(iface, stream, rev237)
        } else {
            decodeIf1(iface, stream)
        }
        return iface
    }

    private fun decodeIf1(iface: InterfaceDefinition, stream: InputBuffer) {
        iface.isIf3 = false
        iface.type = stream.readUnsignedByte()
        iface.menuType = stream.readUnsignedByte()
        iface.contentType = stream.readUnsignedShort()
        iface.originalX = stream.readShort().toInt()
        iface.originalY = stream.readShort().toInt()
        iface.originalWidth = stream.readUnsignedShort()
        iface.originalHeight = stream.readUnsignedShort()
        iface.opacity = stream.readUnsignedByte()
        decodeParentId(iface, stream)

        iface.hoveredSiblingId = stream.readUnsignedShortOrNone()

        val alternateCount = stream.readUnsignedByte()
        if (alternateCount > 0) {
            val operators = IntArray(alternateCount)
            val rhs = IntArray(alternateCount)
            for (i in 0 until alternateCount) {
                operators[i] = stream.readUnsignedByte()
                rhs[i] = stream.readUnsignedShort()
            }
            iface.alternateOperators = operators
            iface.alternateRhs = rhs
        }

        val scriptCount = stream.readUnsignedByte()
        if (scriptCount > 0) {
            iface.clientScripts = Array(scriptCount) {
                val length = stream.readUnsignedShort()
                val bytecode = IntArray(length) {
                    val value = stream.readUnsignedShort()
                    if (value == 0xFFFF) -1 else value
                }
                decodeClientScripts(bytecode)
            }
        }

        if (iface.type == 0) {
            iface.scrollHeight = stream.readUnsignedShort()
            iface.isHidden = stream.readUnsignedByte() == 1
        }

        if (iface.type == 1) {
            stream.readUnsignedShort()
            stream.readUnsignedByte()
        }

        if (iface.type == 2) {
            iface.itemIds = IntArray(iface.originalWidth * iface.originalHeight)
            iface.itemQuantities = IntArray(iface.originalHeight * iface.originalWidth)
            if (stream.readUnsignedByte() == 1) iface.clickMask = iface.clickMask or 268435456
            if (stream.readUnsignedByte() == 1) iface.clickMask = iface.clickMask or 1073741824
            if (stream.readUnsignedByte() == 1) iface.clickMask = iface.clickMask or Integer.MIN_VALUE
            if (stream.readUnsignedByte() == 1) iface.clickMask = iface.clickMask or 536870912
            iface.xPitch = stream.readUnsignedByte()
            iface.yPitch = stream.readUnsignedByte()
            val xOffsets = IntArray(20)
            val yOffsets = IntArray(20)
            val sprites = IntArray(20)
            for (i in 0 until 20) {
                if (stream.readUnsignedByte() == 1) {
                    xOffsets[i] = stream.readShort().toInt()
                    yOffsets[i] = stream.readShort().toInt()
                    sprites[i] = stream.readInt()
                } else {
                    sprites[i] = -1
                }
            }
            iface.xOffsets = xOffsets
            iface.yOffsets = yOffsets
            iface.sprites = sprites
            decodeConfigActions(iface, stream)
        }

        if (iface.type == 3) {
            iface.filled = stream.readUnsignedByte() == 1
        }

        if (iface.type == 4 || iface.type == 1) {
            iface.xTextAlignment = stream.readUnsignedByte()
            iface.yTextAlignment = stream.readUnsignedByte()
            iface.lineHeight = stream.readUnsignedByte()
            iface.fontId = stream.readUnsignedShortOrNone()
            iface.textShadowed = stream.readUnsignedByte() == 1
        }

        if (iface.type == 4) {
            iface.text = stream.readString()
            iface.alternateText = stream.readString()
        }

        if (iface.type == 1 || iface.type == 3 || iface.type == 4) {
            iface.textColor = stream.readInt()
        }

        if (iface.type == 3 || iface.type == 4) {
            iface.alternateTextColor = stream.readInt()
            iface.hoveredTextColor = stream.readInt()
            iface.alternateHoveredTextColor = stream.readInt()
        }

        if (iface.type == 5) {
            iface.spriteId = stream.readInt()
            iface.alternateSpriteId = stream.readInt()
        }

        if (iface.type == 6) {
            iface.modelType = 1
            iface.modelId = stream.readUnsignedShortOrNone()
            iface.alternateModelId = stream.readUnsignedShortOrNone()
            iface.animation = stream.readUnsignedShortOrNone()
            iface.alternateAnimation = stream.readUnsignedShortOrNone()
            iface.modelZoom = stream.readUnsignedShort()
            iface.rotationX = stream.readUnsignedShort()
            iface.rotationZ = stream.readUnsignedShort()
        }

        if (iface.type == 7) {
            iface.itemIds = IntArray(iface.originalWidth * iface.originalHeight)
            iface.itemQuantities = IntArray(iface.originalWidth * iface.originalHeight)
            iface.xTextAlignment = stream.readUnsignedByte()
            iface.fontId = stream.readUnsignedShortOrNone()
            iface.textShadowed = stream.readUnsignedByte() == 1
            iface.textColor = stream.readInt()
            iface.xPitch = stream.readShort().toInt()
            iface.yPitch = stream.readShort().toInt()
            if (stream.readUnsignedByte() == 1) iface.clickMask = iface.clickMask or 1073741824
            decodeConfigActions(iface, stream)
        }

        if (iface.type == 8) {
            iface.text = stream.readString()
        }

        if (iface.menuType == 2 || iface.type == 2) {
            iface.targetVerb = stream.readString()
            iface.spellName = stream.readString()
            iface.clickMask = iface.clickMask or ((stream.readUnsignedShort() and 63) shl 11)
        }

        if (iface.menuType == 1 || iface.menuType == 4 || iface.menuType == 5 || iface.menuType == 6) {
            val tooltip = stream.readString()
            iface.tooltip = if (tooltip.isEmpty()) {
                when (iface.menuType) {
                    1 -> "Ok"
                    4, 5 -> "Select"
                    6 -> "Continue"
                    else -> tooltip
                }
            } else {
                tooltip
            }
        }

        if (iface.menuType == 1 || iface.menuType == 4 || iface.menuType == 5) {
            iface.clickMask = iface.clickMask or 4194304
        }
        if (iface.menuType == 6) {
            iface.clickMask = iface.clickMask or 1
        }
    }

    private fun decodeIf3(iface: InterfaceDefinition, stream: InputBuffer, rev237: Boolean) {
        stream.readUnsignedByte()
        iface.isIf3 = true
        iface.type = stream.readUnsignedByte()
        iface.contentType = stream.readUnsignedShort()
        iface.originalX = stream.readShort().toInt()
        iface.originalY = stream.readShort().toInt()
        iface.originalWidth = stream.readUnsignedShort()
        iface.originalHeight = if (iface.type == 9) stream.readShort().toInt() else stream.readUnsignedShort()
        iface.widthMode = stream.readByte().toInt()
        iface.heightMode = stream.readByte().toInt()
        iface.xPositionMode = stream.readByte().toInt()
        iface.yPositionMode = stream.readByte().toInt()
        decodeParentId(iface, stream)
        iface.isHidden = stream.readUnsignedByte() == 1

        if (iface.type == 0) {
            iface.scrollWidth = stream.readUnsignedShort()
            iface.scrollHeight = stream.readUnsignedShort()
            iface.noClickThrough = stream.readUnsignedByte() == 1
        }

        if (iface.type == 5) {
            iface.spriteId = stream.readInt()
            iface.textureId = stream.readUnsignedShort()
            iface.spriteTiling = stream.readUnsignedByte() == 1
            iface.opacity = stream.readUnsignedByte()
            iface.borderType = stream.readUnsignedByte()
            iface.shadowColor = stream.readInt()
            iface.flippedVertically = stream.readUnsignedByte() == 1
            iface.flippedHorizontally = stream.readUnsignedByte() == 1
        }

        if (iface.type == 6) {
            iface.modelType = 1
            if (rev237 || intModelIds) {
                iface.modelId = stream.readInt()
            } else {
                iface.modelId = stream.readUnsignedShortOrNone()
            }
            iface.offsetX2d = stream.readShort().toInt()
            iface.offsetY2d = stream.readShort().toInt()
            iface.rotationX = stream.readUnsignedShort()
            iface.rotationZ = stream.readUnsignedShort()
            iface.rotationY = stream.readUnsignedShort()
            iface.modelZoom = stream.readUnsignedShort()
            iface.animation = stream.readUnsignedShortOrNone()
            iface.orthogonal = stream.readUnsignedByte() == 1
            stream.readUnsignedShort()
            if (rev237 && (iface.widthMode != 0 || iface.heightMode != 0)) {
                iface.modelHeightOverride = stream.readUnsignedShort()
                stream.readUnsignedShort()
            } else if (!rev237) {
                if (iface.widthMode != 0) iface.modelHeightOverride = stream.readUnsignedShort()
                if (iface.heightMode != 0) stream.readUnsignedShort()
            }
        }

        if (iface.type == 4) {
            iface.fontId = stream.readUnsignedShortOrNone()
            iface.text = stream.readString()
            iface.lineHeight = stream.readUnsignedByte()
            iface.xTextAlignment = stream.readUnsignedByte()
            iface.yTextAlignment = stream.readUnsignedByte()
            iface.textShadowed = stream.readUnsignedByte() == 1
            iface.textColor = stream.readInt()
        }

        if (iface.type == 3) {
            iface.textColor = stream.readInt()
            iface.filled = stream.readUnsignedByte() == 1
            iface.opacity = stream.readUnsignedByte()
        }

        if (iface.type == 9) {
            iface.lineWidth = stream.readUnsignedByte()
            iface.textColor = stream.readInt()
            iface.lineDirection = stream.readUnsignedByte() == 1
        }

        if (rev237 && iface.type == 10) {
            iface.textColor = stream.readInt()
            iface.filled = stream.readUnsignedByte() == 1
            iface.opacity = stream.readUnsignedByte()
            stream.readUnsignedShort()
            stream.readUnsignedShort()
            if (!iface.filled) {
                iface.lineWidth = stream.readUnsignedByte()
            }
        }

        if (stream.remaining() < 3) return
        iface.clickMask = stream.read24BitInt()
        iface.name = stream.readString()
        if (stream.remaining() < 1) return
        val actionCount = stream.readUnsignedByte()
        if (actionCount > 0) {
            iface.actions = Array(actionCount) {
                if (stream.remaining() < 1) "" else stream.readString()
            }
        }
        if (stream.remaining() < 4) return
        iface.dragDeadZone = stream.readUnsignedByte()
        iface.dragDeadTime = stream.readUnsignedByte()
        iface.dragRenderBehavior = stream.readUnsignedByte() == 1
        iface.targetVerb = stream.readString()
        iface.onLoadListener = decodeListener(iface, stream)
        iface.onMouseOverListener = decodeListener(iface, stream)
        iface.onMouseLeaveListener = decodeListener(iface, stream)
        iface.onTargetLeaveListener = decodeListener(iface, stream)
        iface.onTargetEnterListener = decodeListener(iface, stream)
        iface.onVarTransmitListener = decodeListener(iface, stream)
        iface.onInvTransmitListener = decodeListener(iface, stream)
        iface.onStatTransmitListener = decodeListener(iface, stream)
        iface.onTimerListener = decodeListener(iface, stream)
        iface.onOpListener = decodeListener(iface, stream)
        iface.onMouseRepeatListener = decodeListener(iface, stream)
        iface.onClickListener = decodeListener(iface, stream)
        iface.onClickRepeatListener = decodeListener(iface, stream)
        iface.onReleaseListener = decodeListener(iface, stream)
        iface.onHoldListener = decodeListener(iface, stream)
        iface.onDragListener = decodeListener(iface, stream)
        iface.onDragCompleteListener = decodeListener(iface, stream)
        iface.onScrollWheelListener = decodeListener(iface, stream)
        iface.varTransmitTriggers = decodeTriggers(stream)
        iface.invTransmitTriggers = decodeTriggers(stream)
        iface.statTransmitTriggers = decodeTriggers(stream)
    }

    private fun decodeListener(iface: InterfaceDefinition, stream: InputBuffer): Array<Any>? {
        if (stream.remaining() < 1) return null
        val count = stream.readUnsignedByte()
        if (count == 0) return null
        val values = Array<Any>(count) {
            if (stream.remaining() < 1) return@Array 0
            when (stream.readUnsignedByte()) {
                0 -> if (stream.remaining() >= 4) stream.readInt() else 0
                1 -> if (stream.remaining() >= 1) stream.readString() else ""
                else -> 0
            }
        }
        iface.hasListener = true
        return values
    }

    private fun decodeTriggers(stream: InputBuffer): IntArray? {
        if (stream.remaining() < 1) return null
        val count = stream.readUnsignedByte()
        if (count == 0) return null
        return IntArray(count) {
            if (stream.remaining() >= 4) stream.readInt() else 0
        }
    }

    private fun InputBuffer.remaining(): Int = sourceSize - offset

    private fun decodeClientScripts(bytecode: IntArray): Array<ClientScript1Instruction> {
        val opcodes = ClientScript1Instruction.Opcode.entries
        val instructions = ArrayList<ClientScript1Instruction>()
        var i = 0
        while (i < bytecode.size) {
            val opcodeIndex = bytecode[i++]
            if (opcodeIndex !in opcodes.indices) {
                break
            }
            val opcode = opcodes[opcodeIndex]
            val argumentCount = opcode.argumentCount
            val end = (i + argumentCount).coerceAtMost(bytecode.size)
            instructions.add(ClientScript1Instruction(opcode, bytecode.copyOfRange(i, end)))
            i += argumentCount
        }
        return instructions.toTypedArray()
    }

    private fun decodeParentId(iface: InterfaceDefinition, stream: InputBuffer) {
        iface.parentId = stream.readUnsignedShort()
        if (iface.parentId == 0xFFFF) {
            iface.parentId = -1
        } else {
            iface.parentId += iface.id and 0xFFFF.inv()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun decodeConfigActions(iface: InterfaceDefinition, stream: InputBuffer) {
        val actions = arrayOfNulls<String>(5)
        for (i in 0 until 5) {
            val action = stream.readString()
            if (action.isNotEmpty()) {
                actions[i] = action
                iface.clickMask = iface.clickMask or (1 shl i + 23)
            }
        }
        iface.configActions = actions as Array<String>
    }

    private fun InputBuffer.readUnsignedShortOrNone(): Int {
        val value = readUnsignedShort()
        return if (value == 0xFFFF) -1 else value
    }

    companion object {
        private const val MAGIC = 0xAABBCCDD
        private const val MAGIC_SIZE = 4
        private const val INT_MODEL_REVISION = 1_773_137_432

        fun hasRev237Magic(data: ByteArray): Boolean =
            data.size > MAGIC_SIZE &&
                data[0] == (MAGIC shr 24).toByte() &&
                data[1] == (MAGIC shr 16).toByte() &&
                data[2] == (MAGIC shr 8).toByte() &&
                data[3] == MAGIC.toByte()
    }
}
