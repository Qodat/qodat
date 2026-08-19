package stan.qodat.scene.runescape.model

import javafx.scene.paint.Color
import javafx.scene.shape.DrawMode
import qodat.cache.definition.ModelDefinition
import stan.qodat.util.DISTINCT_COLORS

/**
 * How a [Model] should be presented in the 3D view.
 */
enum class ModelViewMode(private val label: String) {
    FILL("Fill"),
    LINE("Wireframe"),
    VERTEX_SKIN("Vertex Skin"),
    FACE_SKIN("Face Skin"),
    PRIORITY("Priority");

    override fun toString(): String = label

    fun toDrawMode(): DrawMode =
        if (this == LINE) DrawMode.LINE else DrawMode.FILL

    fun isDiagnostic(): Boolean =
        this == VERTEX_SKIN || this == FACE_SKIN || this == PRIORITY
}

fun distinctColor(index: Int): Color {
    val color = DISTINCT_COLORS[Math.floorMod(index, DISTINCT_COLORS.size)]
    return Color.color(color.red, color.green, color.blue)
}

fun ModelDefinition.colorForViewMode(viewMode: ModelViewMode, face: Int, fallback: Color): Color {
    return when (viewMode) {
        ModelViewMode.VERTEX_SKIN -> vertexSkinFaceColor(face) ?: fallback
        ModelViewMode.FACE_SKIN -> {
            val skins = getFaceSkins() ?: return fallback
            distinctColor(skins[face])
        }
        ModelViewMode.PRIORITY -> {
            val priority = getFacePriorities()?.get(face) ?: getPriority()
            distinctColor(priority.toInt() and 0xFF)
        }
        ModelViewMode.FILL, ModelViewMode.LINE -> fallback
    }
}

private fun ModelDefinition.vertexSkinFaceColor(face: Int): Color? {
    val skins = getVertexSkins() ?: return null
    val s1 = skins[getFaceVertexIndices1()[face]]
    val s2 = skins[getFaceVertexIndices2()[face]]
    val s3 = skins[getFaceVertexIndices3()[face]]
    if (s1 == s2 && s2 == s3)
        return distinctColor(s1)
    // Mixed-weight faces blend the three vertex-group colors so they stay
    // visible without being mistaken for a single-group triangle.
    val c1 = distinctColor(s1)
    val c2 = distinctColor(s2)
    val c3 = distinctColor(s3)
    return Color.color(
        (c1.red + c2.red + c3.red) / 3.0,
        (c1.green + c2.green + c3.green) / 3.0,
        (c1.blue + c2.blue + c3.blue) / 3.0
    )
}
