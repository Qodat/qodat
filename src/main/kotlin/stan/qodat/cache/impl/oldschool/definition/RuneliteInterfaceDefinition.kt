package stan.qodat.cache.impl.oldschool.definition

import qodat.cache.definition.ClientScript1Instruction
import qodat.cache.definition.ClientScript1Instruction.Opcode
import qodat.cache.definition.InterfaceDefinition
import net.runelite.cache.definitions.ClientScript1Instruction as RuneliteClientScript1Instruction

class RuneliteInterfaceDefinition(definition: net.runelite.cache.definitions.InterfaceDefinition) : InterfaceDefinition {

    override var id: Int = definition.id
    override var isIf3: Boolean = definition.isIf3
    override var type: Int = definition.type
    override var contentType: Int = definition.contentType
    override var originalX: Int = definition.originalX
    override var originalY: Int = definition.originalY
    override var originalWidth: Int = definition.originalWidth
    override var originalHeight: Int = definition.originalHeight
    override var widthMode: Int = definition.widthMode
    override var heightMode: Int = definition.heightMode
    override var xPositionMode: Int = definition.xPositionMode
    override var yPositionMode: Int = definition.yPositionMode
    override var parentId: Int = definition.parentId
    override var isHidden: Boolean = definition.isHidden
    override var scrollWidth: Int = definition.scrollWidth
    override var scrollHeight: Int = definition.scrollHeight
    override var noClickThrough: Boolean = definition.noClickThrough
    override var spriteId: Int = definition.spriteId
    override var textureId: Int = definition.textureId
    override var spriteTiling: Boolean = definition.spriteTiling
    override var opacity: Int = definition.opacity
    override var borderType: Int = definition.borderType
    override var shadowColor: Int = definition.shadowColor
    override var flippedVertically: Boolean = definition.flippedVertically
    override var flippedHorizontally: Boolean = definition.flippedHorizontally
    override var modelType: Int = definition.modelType
    override var modelId: Int = definition.modelId
    override var offsetX2d: Int = definition.offsetX2d
    override var offsetY2d: Int = definition.offsetY2d
    override var rotationX: Int = definition.rotationX
    override var rotationY: Int = definition.rotationY
    override var rotationZ: Int = definition.rotationZ
    override var modelZoom: Int = definition.modelZoom
    override var animation: Int = definition.animation
    override var orthogonal: Boolean = definition.orthogonal
    override var modelHeightOverride: Int = definition.modelHeightOverride
    override var fontId: Int = definition.fontId
    override var text: String? = definition.text
    override var lineHeight: Int = definition.lineHeight
    override var xTextAlignment: Int = definition.xTextAlignment
    override var yTextAlignment: Int = definition.yTextAlignment
    override var textShadowed: Boolean = definition.textShadowed
    override var textColor: Int = definition.textColor
    override var filled: Boolean = definition.filled
    override var lineWidth: Int = definition.lineWidth
    override var lineDirection: Boolean = definition.lineDirection
    override var clickMask: Int = definition.clickMask
    override var name: String? = definition.name
    override var actions: Array<String>? = definition.actions
    override var dragDeadZone: Int = definition.dragDeadZone
    override var dragDeadTime: Int = definition.dragDeadTime
    override var dragRenderBehavior: Boolean = definition.dragRenderBehavior
    override var targetVerb: String? = definition.targetVerb
    override var onLoadListener = definition.onLoadListener
    override var onMouseOverListener = definition.onMouseOverListener
    override var onMouseLeaveListener = definition.onMouseLeaveListener
    override var onTargetLeaveListener = definition.onTargetLeaveListener
    override var onTargetEnterListener = definition.onTargetEnterListener
    override var onVarTransmitListener = definition.onVarTransmitListener
    override var onInvTransmitListener = definition.onInvTransmitListener
    override var onStatTransmitListener = definition.onStatTransmitListener
    override var onTimerListener = definition.onTimerListener
    override var onOpListener = definition.onOpListener
    override var onMouseRepeatListener = definition.onMouseRepeatListener
    override var onClickListener = definition.onClickListener
    override var onClickRepeatListener = definition.onClickRepeatListener
    override var onReleaseListener = definition.onReleaseListener
    override var onHoldListener = definition.onHoldListener
    override var onDragListener = definition.onDragListener
    override var onDragCompleteListener = definition.onDragCompleteListener
    override var onScrollWheelListener = definition.onScrollWheelListener
    override var varTransmitTriggers = definition.varTransmitTriggers
    override var invTransmitTriggers = definition.invTransmitTriggers
    override var statTransmitTriggers = definition.statTransmitTriggers
    override var hasListener: Boolean = definition.hasListener
    override var menuType: Int = definition.menuType
    override var hoveredSiblingId: Int = definition.hoveredSiblingId
    override var alternateOperators = definition.alternateOperators
    override var alternateRhs = definition.alternateRhs
    override var clientScripts = definition.clientScripts?.map { scripts ->
        Array(scripts.size) {
            val script = scripts[it]
            val opcode = mapOpcode(script.opcode)
            ClientScript1Instruction(opcode, script.operands)
        }
    }?.toTypedArray()
    override var itemIds = definition.itemIds
    override var itemQuantities = definition.itemQuantities
    override var xPitch: Int = definition.xPitch
    override var yPitch: Int = definition.yPitch
    override var xOffsets = definition.xOffsets
    override var yOffsets = definition.yOffsets
    override var sprites = definition.sprites
    override var configActions = definition.configActions
    override var alternateText: String? = definition.alternateText
    override var alternateTextColor: Int = definition.alternateTextColor
    override var hoveredTextColor: Int = definition.hoveredTextColor
    override var alternateHoveredTextColor: Int = definition.alternateHoveredTextColor
    override var alternateSpriteId: Int = definition.alternateSpriteId
    override var alternateModelId: Int = definition.alternateModelId
    override var alternateAnimation: Int = definition.alternateAnimation
    override var spellName: String? = definition.spellName
    override var tooltip: String? = definition.tooltip

    private fun mapOpcode(opcode: RuneliteClientScript1Instruction.Opcode?): Opcode? = when (opcode) {
        RuneliteClientScript1Instruction.Opcode.RETURN -> Opcode.RETURN
        RuneliteClientScript1Instruction.Opcode.BOOSTED_SKILL_LEVELS -> Opcode.BOOSTED_SKILL_LEVELS
        RuneliteClientScript1Instruction.Opcode.REAL_SKILL_LEVELS -> Opcode.REAL_SKILL_LEVELS
        RuneliteClientScript1Instruction.Opcode.SKILL_EXPERIENCE -> Opcode.SKILL_EXPERIENCE
        RuneliteClientScript1Instruction.Opcode.WIDGET_CONTAINS_ITEM_GET_QUANTITY -> Opcode.WIDGET_CONTAINS_ITEM_GET_QUANTITY
        RuneliteClientScript1Instruction.Opcode.VARP -> Opcode.VARP
        RuneliteClientScript1Instruction.Opcode.EXPERIENCE_AT_LEVEL_FOR_SKILL -> Opcode.EXPERIENCE_AT_LEVEL_FOR_SKILL
        RuneliteClientScript1Instruction.Opcode.VARP_TIMES_469 -> Opcode.VARP_TIMES_469
        RuneliteClientScript1Instruction.Opcode.COMBAT_LEVEL -> Opcode.COMBAT_LEVEL
        RuneliteClientScript1Instruction.Opcode.TOTAL_LEVEL -> Opcode.TOTAL_LEVEL
        RuneliteClientScript1Instruction.Opcode.WIDGET_CONTAINS_ITEM_STAR -> Opcode.WIDGET_CONTAINS_ITEM_STAR
        RuneliteClientScript1Instruction.Opcode.RUN_ENERGY -> Opcode.RUN_ENERGY
        RuneliteClientScript1Instruction.Opcode.WEIGHT -> Opcode.WEIGHT
        RuneliteClientScript1Instruction.Opcode.VARP_TESTBIT -> Opcode.VARP_TESTBIT
        RuneliteClientScript1Instruction.Opcode.VARBIT -> Opcode.VARBIT
        RuneliteClientScript1Instruction.Opcode.MINUS -> Opcode.MINUS
        RuneliteClientScript1Instruction.Opcode.DIV -> Opcode.DIV
        RuneliteClientScript1Instruction.Opcode.MUL -> Opcode.MUL
        RuneliteClientScript1Instruction.Opcode.WORLD_X -> Opcode.WORLD_X
        RuneliteClientScript1Instruction.Opcode.WORLD_Y -> Opcode.WORLD_Y
        RuneliteClientScript1Instruction.Opcode.CONSTANT -> Opcode.CONSTANT
        else -> null
    }
}