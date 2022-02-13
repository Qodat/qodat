package stan.qodat.scene.control.export.wavefront

import qodat.cache.definition.ModelDefinition
import java.awt.Color

fun ModelDefinition.getFaceMaterial(face: Int): WaveFrontMaterial.Color {
    val color = rs2hsbToColor(
        getFaceColors()[face].toInt()
    )
    val r = color.red / 255.0
    val g = color.green / 255.0
    val b = color.blue / 255.0
    val a = getFaceAlphas()?.get(face)?.let { it.toInt() and 0xFF }?.div(255.0) ?: 0.0
    val material = WaveFrontMaterial.Color(r, g, b, a)
    return material
}

fun ModelDefinition.getFaceMaterials() = Array<WaveFrontMaterial>(getFaceCount()) { face ->
    getFaceMaterial(face)
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


