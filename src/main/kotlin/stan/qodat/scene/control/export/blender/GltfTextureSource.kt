package stan.qodat.scene.control.export.blender

import qodat.cache.Cache
import qodat.cache.definition.TextureDefinition
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.sqrt

/**
 * A cache texture packed as PNG for a glTF `image` / `texture`.
 *
 * [pixels] are client RGB with `0` meaning transparent, same as
 * [stan.qodat.scene.paint.TextureMaterial].
 */
data class GltfTextureImage(
    val id: Int,
    val width: Int,
    val height: Int,
    val png: ByteArray,
    val hasAlpha: Boolean,
    val animationDirection: Int = 0,
    val animationSpeed: Int = 0,
    val spriteFileId: Int = -1,
)

fun interface GltfTextureSource {
    fun load(textureId: Int): GltfTextureImage?

    companion object {
        val NONE = GltfTextureSource { null }

        fun from(cache: Cache?): GltfTextureSource {
            if (cache == null) return NONE
            return GltfTextureSource { id ->
                try {
                    encode(cache.getTexture(id))
                } catch (_: Exception) {
                    null
                }
            }
        }

        fun encode(definition: TextureDefinition): GltfTextureImage? {
            val src = definition.pixels
            if (src.isEmpty()) return null
            val size = sqrt(src.size.toDouble()).toInt()
            if (size <= 0 || size * size != src.size) return null
            var hasAlpha = false
            val argb = IntArray(src.size) { i ->
                val rgb = src[i]
                if (rgb == 0) {
                    hasAlpha = true
                    0
                } else {
                    rgb or 0xFF000000.toInt()
                }
            }
            val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
            image.setRGB(0, 0, size, size, argb, 0, size)
            val png = ByteArrayOutputStream().use { out ->
                if (!ImageIO.write(image, "png", out)) return null
                out.toByteArray()
            }
            return GltfTextureImage(
                id = definition.id,
                width = size,
                height = size,
                png = png,
                hasAlpha = hasAlpha,
                animationDirection = definition.animationDirection,
                animationSpeed = definition.animationSpeed,
                spriteFileId = definition.fileIds.firstOrNull() ?: -1,
            )
        }
    }
}

internal fun distinctFaceTextureIds(faceTextures: ShortArray?): IntArray {
    if (faceTextures == null) return intArrayOf()
    return faceTextures
        .map { it.toInt() }
        .filter { it != -1 && (it and 0xFFFF) != 0xFFFF }
        .distinct()
        .sorted()
        .toIntArray()
}

internal fun faceTextureId(faceTextures: ShortArray?, face: Int): Int {
    val raw = faceTextures?.getOrNull(face)?.toInt() ?: return -1
    return if (raw == -1 || (raw and 0xFFFF) == 0xFFFF) -1 else raw
}
