package stan.qodat.cache.impl.oldschool.definition

import qodat.cache.definition.SpriteDefinition
import qodat.cache.definition.TextureDefinition as QodatTextureDefinition

/**
 * OSRS texture (index 9, archive 0) definition.
 *
 * [fileIds] are sprite group ids sampled by [computePixels] (`method2680`).
 * [field1780]/[field1781]/[field1786] exist only on the pre-rev233 multi-file
 * table; the compact single-file layout leaves them unset.
 */
class TextureDefinition(override var id: Int) : QodatTextureDefinition {

    override var fileIds: IntArray = intArrayOf()
    override var pixels: IntArray = intArrayOf()
    override var animationDirection: Int = 0
    override var animationSpeed: Int = 0

    var missingColor = 0
    var field1778 = false
    var field1780: IntArray? = null
    var field1781: IntArray? = null
    var field1786: IntArray? = null

    /**
     * Sample sprite pixels into [pixels] at [size]×[size], matching client
     * `Texture.method2680`. [brightness] is the gamma applied to the palette
     * (`1.0` leaves colours unchanged).
     */
    fun computePixels(
        brightness: Double,
        size: Int,
        spriteProvider: (spriteId: Int, frameId: Int) -> SpriteDefinition?,
    ): Boolean {
        val pixelCount = size * size
        pixels = IntArray(pixelCount)
        for (fileIndex in fileIds.indices) {
            val sprite = spriteProvider(fileIds[fileIndex], 0)
                ?: throw IllegalArgumentException("Sprite not found ${fileIds[fileIndex]}:0")
            val palette = sprite.palette.copyOf()
            applyLegacyTint(fileIndex, palette)
            for (i in palette.indices) {
                palette[i] = adjustRgb(palette[i], brightness)
            }
            val combineMode = if (fileIndex == 0) 0 else field1780!![fileIndex - 1]
            if (combineMode != 0) continue
            blitSprite(normalizedPixelIdx(sprite), palette, sprite.maxWidth, size, pixels)
        }
        return true
    }

    private fun applyLegacyTint(fileIndex: Int, palette: IntArray) {
        val tints = field1786 ?: return
        val tint = tints[fileIndex]
        if (tint and -0x1000000 != 0x03000000) return
        val rb = tint and 0x00FF00FF
        val g = tint shr 8 and 255
        for (i in palette.indices) {
            val rgb = palette[i]
            if (rgb shr 8 != (rgb and 0xFFFF)) continue
            val grey = rgb and 255
            palette[i] = rb * grey shr 8 and 0x00FF00FF or (g * grey and 0xFF00)
        }
    }

    companion object {
        internal fun adjustRgb(rgb: Int, brightness: Double): Int {
            val r = Math.pow((rgb shr 16).toDouble() / 256.0, brightness)
            val g = Math.pow((rgb shr 8 and 255).toDouble() / 256.0, brightness)
            val b = Math.pow((rgb and 255).toDouble() / 256.0, brightness)
            return (b * 256.0).toInt() + ((g * 256.0).toInt() shl 8) + ((r * 256.0).toInt() shl 16)
        }

        internal fun normalizedPixelIdx(sprite: SpriteDefinition): ByteArray {
            if (sprite.width == sprite.maxWidth && sprite.height == sprite.maxHeight) {
                return sprite.pixelIdx
            }
            val out = ByteArray(sprite.maxWidth * sprite.maxHeight)
            var src = 0
            for (y in 0 until sprite.height) {
                for (x in 0 until sprite.width) {
                    out[x + (y + sprite.offsetY) * sprite.maxWidth + sprite.offsetX] = sprite.pixelIdx[src++]
                }
            }
            return out
        }

        internal fun blitSprite(
            pixelIdx: ByteArray,
            palette: IntArray,
            maxWidth: Int,
            size: Int,
            dest: IntArray,
        ) {
            val pixelCount = size * size
            when {
                size == maxWidth -> {
                    for (i in 0 until pixelCount) {
                        dest[i] = palette[pixelIdx[i].toInt() and 255]
                    }
                }
                maxWidth == 64 && size == 128 -> {
                    var i = 0
                    for (row in 0 until size) {
                        for (col in 0 until size) {
                            dest[i++] = palette[pixelIdx[(row shr 1 shl 6) + (col shr 1)].toInt() and 255]
                        }
                    }
                }
                maxWidth == 128 && size == 64 -> {
                    var i = 0
                    for (row in 0 until size) {
                        for (col in 0 until size) {
                            dest[i++] = palette[pixelIdx[(col shl 1) + (row shl 1 shl 7)].toInt() and 255]
                        }
                    }
                }
                else -> throw RuntimeException()
            }
        }
    }
}
