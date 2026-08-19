package stan.qodat.scene.paint

import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * The three corner colours of a single face.
 */
data class FaceTint(val corner1: Color, val corner2: Color, val corner3: Color) {

    val isFlat get() = corner1 == corner2 && corner2 == corner3

    companion object {
        fun flat(color: Color) = FaceTint(color, color, color)
    }
}

/**
 * A [PhongMaterial] whose diffuse map encodes the colours of every face in a model.
 *
 * JavaFX cannot colour individual triangles of a mesh, let alone interpolate a colour across one,
 * so colours are baked into a texture instead. Each face gets a 2x2 tile; its three corners map to
 * three of the tile's texels and the fourth is chosen so that the bilinear filter degenerates into
 * a plain affine interpolation. That reproduces Gouraud shading exactly rather than approximating
 * it, and because sampling stays within the tile's texel centres, neighbouring tiles never bleed.
 *
 * Faces that share the same corner colours share a tile, so a flat shaded model costs no more than
 * one tile per distinct colour.
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/09/2019
 */
class AtlasMaterial : PhongMaterial() {

    private var faceTiles = IntArray(0)
    private var columns = 1
    private var width = 2
    private var height = 2

    val imageProperty = SimpleObjectProperty<WritableImage>()

    init {
        diffuseMapProperty().bind(imageProperty)
    }

    /**
     * Rebuilds the diffuse map from the colours of every face.
     *
     * @param tints one entry per face, in face order.
     */
    fun setFaceTints(tints: Array<FaceTint>) {

        val tileIndices = LinkedHashMap<FaceTint, Int>()
        faceTiles = IntArray(tints.size) { face ->
            tileIndices.getOrPut(tints[face]) { tileIndices.size }
        }

        val tileCount = tileIndices.size.coerceAtLeast(1)
        columns = ceil(sqrt(tileCount.toDouble())).toInt().coerceAtLeast(1)
        val rows = ceil(tileCount.toDouble() / columns).toInt().coerceAtLeast(1)
        width = columns * TILE_SIZE
        height = rows * TILE_SIZE

        val image = WritableImage(width, height)
        val writer = image.pixelWriter
        for ((tint, tile) in tileIndices) {
            val x = (tile % columns) * TILE_SIZE
            val y = (tile / columns) * TILE_SIZE
            writer.setColor(x, y, tint.corner1)
            writer.setColor(x + 1, y, tint.corner2)
            writer.setColor(x, y + 1, tint.corner3)
            writer.setColor(x + 1, y + 1, complement(tint))
        }
        imageProperty.set(image)
    }

    /**
     * The U coordinate of the given corner (0, 1 or 2) of the given face.
     */
    fun getU(face: Int, corner: Int): Float {
        if (face !in faceTiles.indices)
            return 0F
        val x = (faceTiles[face] % columns) * TILE_SIZE
        return ((x + if (corner == 1) 1.5 else 0.5) / width).toFloat()
    }

    /**
     * The V coordinate of the given corner (0, 1 or 2) of the given face.
     */
    fun getV(face: Int, corner: Int): Float {
        if (face !in faceTiles.indices)
            return 0F
        val y = (faceTiles[face] / columns) * TILE_SIZE
        return ((y + if (corner == 2) 1.5 else 0.5) / height).toFloat()
    }

    /**
     * The fourth texel of a tile, positioned so that bilinear filtering across the tile has no
     * cross term and therefore interpolates the three corners linearly.
     */
    private fun complement(tint: FaceTint) = Color.color(
        clamp(tint.corner2.red + tint.corner3.red - tint.corner1.red),
        clamp(tint.corner2.green + tint.corner3.green - tint.corner1.green),
        clamp(tint.corner2.blue + tint.corner3.blue - tint.corner1.blue),
        clamp(tint.corner2.opacity + tint.corner3.opacity - tint.corner1.opacity)
    )

    private fun clamp(value: Double) = value.coerceIn(0.0, 1.0)

    private companion object {
        const val TILE_SIZE = 2
    }
}
