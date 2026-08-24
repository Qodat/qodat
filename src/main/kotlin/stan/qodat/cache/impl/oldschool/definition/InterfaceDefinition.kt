package stan.qodat.cache.impl.oldschool.definition

import qodat.cache.definition.ClientScript1Instruction
import qodat.cache.definition.InterfaceDefinition as QodatInterfaceDefinition

/**
 * OSRS interface (index 3) widget. IF1, IF3, and rev237 IF3 (`0xAABBCCDD`)
 * share this type; the newer decoder fills older fields and leaves newer
 * ones at defaults.
 */
class InterfaceDefinition : QodatInterfaceDefinition {

    override var id = -1
    override var isIf3 = false
    override var type = 0
    override var contentType = 0
    override var originalX = 0
    override var originalY = 0
    override var originalWidth = 0
    override var originalHeight = 0
    override var widthMode = 0
    override var heightMode = 0
    override var xPositionMode = 0
    override var yPositionMode = 0
    override var parentId = -1
    override var isHidden = false
    override var scrollWidth = 0
    override var scrollHeight = 0
    override var noClickThrough = false
    override var spriteId = -1
    override var textureId = 0
    override var spriteTiling = false
    override var opacity = 0
    override var borderType = 0
    override var shadowColor = 0
    override var flippedVertically = false
    override var flippedHorizontally = false
    override var modelType = 1
    override var modelId = -1
    override var offsetX2d = 0
    override var offsetY2d = 0
    override var rotationX = 0
    override var rotationY = 0
    override var rotationZ = 0
    override var modelZoom = 100
    override var animation = -1
    override var orthogonal = false
    override var modelHeightOverride = 0
    override var fontId = -1
    override var text: String? = ""
    override var lineHeight = 0
    override var xTextAlignment = 0
    override var yTextAlignment = 0
    override var textShadowed = false
    override var textColor = 0
    override var filled = false
    override var lineWidth = 1
    override var lineDirection = false
    override var clickMask = 0
    override var name: String? = ""
    override var actions: Array<String>? = null
    override var dragDeadZone = 0
    override var dragDeadTime = 0
    override var dragRenderBehavior = false
    override var targetVerb: String? = ""
    override var onLoadListener: Array<Any>? = null
    override var onMouseOverListener: Array<Any>? = null
    override var onMouseLeaveListener: Array<Any>? = null
    override var onTargetLeaveListener: Array<Any>? = null
    override var onTargetEnterListener: Array<Any>? = null
    override var onVarTransmitListener: Array<Any>? = null
    override var onInvTransmitListener: Array<Any>? = null
    override var onStatTransmitListener: Array<Any>? = null
    override var onTimerListener: Array<Any>? = null
    override var onOpListener: Array<Any>? = null
    override var onMouseRepeatListener: Array<Any>? = null
    override var onClickListener: Array<Any>? = null
    override var onClickRepeatListener: Array<Any>? = null
    override var onReleaseListener: Array<Any>? = null
    override var onHoldListener: Array<Any>? = null
    override var onDragListener: Array<Any>? = null
    override var onDragCompleteListener: Array<Any>? = null
    override var onScrollWheelListener: Array<Any>? = null
    override var varTransmitTriggers: IntArray? = null
    override var invTransmitTriggers: IntArray? = null
    override var statTransmitTriggers: IntArray? = null
    override var hasListener = false
    override var menuType = 0
    override var hoveredSiblingId = 0
    override var alternateOperators: IntArray? = null
    override var alternateRhs: IntArray? = null
    override var clientScripts: Array<Array<ClientScript1Instruction>>? = null
    override var itemIds: IntArray? = null
    override var itemQuantities: IntArray? = null
    override var xPitch = 0
    override var yPitch = 0
    override var xOffsets: IntArray? = null
    override var yOffsets: IntArray? = null
    override var sprites: IntArray? = null
    override var configActions: Array<String>? = null
    override var alternateText: String? = ""
    override var alternateTextColor = 0
    override var hoveredTextColor = 0
    override var alternateHoveredTextColor = 0
    override var alternateSpriteId = -1
    override var alternateModelId = -1
    override var alternateAnimation = -1
    override var spellName: String? = ""
    override var tooltip: String? = "Ok"
}
