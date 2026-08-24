package stan.qodat.cache.impl.oldschool.definition

import stan.qodat.cache.impl.oldschool.loader.SpriteLoader
import qodat.cache.definition.SpriteDefinition as QodatSpriteDefinition

/**
 * OSRS sprite (index 8) definition.
 *
 * Frames in one archive share [palette] and [maxWidth]/[maxHeight]. Pixel
 * rows stay in the archive bytes until [pixels] or [pixelIdx] is read so the
 * sprite list does not allocate 14k ARGB bitmaps at cache open.
 */
class SpriteDefinition(
    override var id: Int = -1,
    override var frame: Int = 0,
) : QodatSpriteDefinition {

    override var offsetX = 0
    override var offsetY = 0
    override var width = 0
    override var height = 0
    override var maxWidth = 0
    override var maxHeight = 0
    override var palette = EMPTY_INTS

    internal var pixelSource: ByteArray? = null
    internal var pixelDataOffset = 0
    internal var flags = 0

    @Volatile
    private var inflated = false
    private var pixelsField = EMPTY_INTS
    private var pixelIdxField = EMPTY_BYTES

    override var pixels: IntArray
        get() {
            inflatePixels()
            return pixelsField
        }
        set(value) {
            pixelsField = value
            inflated = true
        }

    override var pixelIdx: ByteArray
        get() {
            inflatePixels()
            return pixelIdxField
        }
        set(value) {
            pixelIdxField = value
        }

    internal val isPixelDataInflated: Boolean
        get() = inflated

    fun inflatePixels() {
        if (inflated) return
        val source = pixelSource
        if (source == null) {
            inflated = true
            return
        }
        synchronized(this) {
            if (inflated) return
            SpriteLoader.inflate(this, source)
            inflated = true
            pixelSource = null
        }
    }

    internal fun assignInflated(pixels: IntArray, pixelIdx: ByteArray) {
        pixelsField = pixels
        pixelIdxField = pixelIdx
    }

    companion object {
        internal val EMPTY_INTS = IntArray(0)
        internal val EMPTY_BYTES = ByteArray(0)
    }
}
