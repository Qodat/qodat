package stan.qodat.cache.impl.oldschool.definition

import qodat.cache.definition.SpriteDefinition as QodatSpriteDefinition

/**
 * OSRS sprite (index 8) definition.
 *
 * Frames in one archive share [palette] and [maxWidth]/[maxHeight]; each frame
 * has its own [pixelIdx], [pixels], and offsets.
 */
class SpriteDefinition(
    override var id: Int = -1,
    override var frame: Int = 0,
) : QodatSpriteDefinition {

    override var offsetX = 0
    override var offsetY = 0
    override var width = 0
    override var height = 0
    override var pixels = IntArray(0)
    override var maxWidth = 0
    override var maxHeight = 0
    override var pixelIdx = ByteArray(0)
    override var palette = IntArray(0)
}
