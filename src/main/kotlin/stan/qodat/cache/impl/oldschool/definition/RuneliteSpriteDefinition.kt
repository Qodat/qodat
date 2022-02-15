package stan.qodat.cache.impl.oldschool.definition

import qodat.cache.definition.SpriteDefinition

class RuneliteSpriteDefinition(definition: net.runelite.cache.definitions.SpriteDefinition) : SpriteDefinition {
    override val id: Int = definition.id
    override val frame: Int = definition.frame
    override val offsetX: Int = definition.offsetX
    override val offsetY: Int = definition.offsetY
    override val width: Int = definition.width
    override val height: Int = definition.height
    override val pixels: IntArray = definition.pixels
    override val maxWidth: Int = definition.maxWidth
    override val maxHeight: Int = definition.maxHeight
    override var pixelIdx: ByteArray = definition.pixelIdx
    override var palette: IntArray = definition.palette
    override fun toString(): String {
        return "RuneliteSpriteDefinition(id=$id, frame=$frame, offsetX=$offsetX, offsetY=$offsetY, width=$width, height=$height, pixels=${pixels.contentToString()}, maxWidth=$maxWidth, maxHeight=$maxHeight, pixelIdx=${pixelIdx.contentToString()}, palette=${palette.contentToString()})"
    }


}