package stan.qodat.cache.impl.oldschool.loader

import stan.qodat.cache.impl.oldschool.definition.SpriteDefinition

/**
 * OSRS sprite (index 8) decoder.
 *
 * Trailer layout is shared across revisions: sprite count, max size, palette
 * length, then per-frame offsets/sizes sit at the end of the archive. Pixel
 * rows start at offset 0. [FLAG_VERTICAL] and [FLAG_ALPHA] coexist so a newer
 * decoder reads older caches that omit alpha.
 */
class SpriteLoader {

    fun load(id: Int, b: ByteArray): Array<SpriteDefinition> {
        val input = DecodeCursor(b)
        val length = b.size

        input.offset = length - 2
        val spriteCount = input.readUnsignedShort()
        val sprites = Array(spriteCount) { SpriteDefinition(id, it) }

        input.offset = length - 7 - spriteCount * 8
        val maxWidth = input.readUnsignedShort()
        val maxHeight = input.readUnsignedShort()
        val paletteLength = input.readUnsignedByte() + 1
        for (sprite in sprites) {
            sprite.maxWidth = maxWidth
            sprite.maxHeight = maxHeight
        }
        for (sprite in sprites) sprite.offsetX = input.readUnsignedShort()
        for (sprite in sprites) sprite.offsetY = input.readUnsignedShort()
        for (sprite in sprites) sprite.width = input.readUnsignedShort()
        for (sprite in sprites) sprite.height = input.readUnsignedShort()

        input.offset = length - 7 - spriteCount * 8 - (paletteLength - 1) * 3
        val palette = IntArray(paletteLength)
        for (i in 1 until paletteLength) {
            palette[i] = input.read24BitInt()
            if (palette[i] == 0) palette[i] = 1
        }

        input.offset = 0
        for (sprite in sprites) {
            val dimension = sprite.width * sprite.height
            val pixelIdx = ByteArray(dimension)
            val pixelAlphas = ByteArray(dimension)
            sprite.pixelIdx = pixelIdx
            sprite.palette = palette

            val flags = input.readUnsignedByte()
            val vertical = flags and FLAG_VERTICAL != 0
            readIndexed(input, pixelIdx, sprite.width, sprite.height, vertical)
            if (flags and FLAG_ALPHA != 0) {
                readIndexed(input, pixelAlphas, sprite.width, sprite.height, vertical)
            }

            val pixels = IntArray(dimension)
            for (j in 0 until dimension) {
                val idxByte = pixelIdx[j]
                val index = idxByte.toInt() and 0xFF
                val alpha = if (idxByte.toInt() != 0) 0xFF else pixelAlphas[j].toInt()
                pixels[j] = palette[index] or (alpha shl 24)
            }
            sprite.pixels = pixels
        }
        return sprites
    }

    private fun readIndexed(
        input: DecodeCursor,
        dest: ByteArray,
        width: Int,
        height: Int,
        vertical: Boolean,
    ) {
        if (!vertical) {
            input.readBytes(dest)
            return
        }
        val raw = input.raw()
        var o = input.offset
        for (x in 0 until width) {
            for (y in 0 until height) {
                dest[width * y + x] = raw[o++]
            }
        }
        input.offset = o
    }

    companion object {
        const val FLAG_VERTICAL = 0b01
        const val FLAG_ALPHA = 0b10
    }
}
