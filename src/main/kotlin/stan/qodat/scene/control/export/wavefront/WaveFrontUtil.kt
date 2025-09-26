package stan.qodat.scene.control.export.wavefront

import qodat.cache.definition.ModelDefinition
import stan.qodat.cache.impl.displee.DispleeCache
import java.awt.Color

fun ModelDefinition.getFaceMaterial(face: Int, recolorMap: Map<Short, Short>? = null): WaveFrontMaterial {
    val a = getFaceAlphas()?.get(face)?.let { it.toInt() and 0xFF }?.div(255.0) ?: 0.0
    val textureId = getFaceTextures()?.getOrNull(face)?.toInt()?:-1
    return if (textureId == -1) {
        val color = rs2hsbToColor(getFaceColors()[face].let { recolorMap?.get(it)?:it }.toInt())
        val r = color.red / 255.0
        val g = color.green / 255.0
        val b = color.blue / 255.0
        WaveFrontMaterial.Color(r, g, b, a)
    } else {
        val texture = DispleeCache.getTexture(textureId)
        val spriteFileId = texture.fileIds[0]
        WaveFrontMaterial.Texture(spriteFileId, a)
    }
}

fun ModelDefinition.getFaceMaterials(recolorMap: Map<Short, Short>? = null) = Array(getFaceCount()) { face ->
    getFaceMaterial(face, recolorMap)
}

private fun rs2hsbToColor(hsb: Int): Color {
    val decode_hue = hsb shr 10 and 0x3f
    val decode_saturation = hsb shr 7 and 0x07
    val decode_brightness = hsb and 0x7f
    return Color.getHSBColor(
        decode_hue.toFloat() / 63,
        decode_saturation.toFloat() / 7,
        decode_brightness.toFloat() / 127
    )
}


