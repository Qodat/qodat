package stan.qodat.scene.runescape

import qodat.cache.definition.ModelDefinition
import qodat.cache.models.VertexNormal
import kotlin.math.sqrt

const val MAX_VIEW_DISTANCE = 8192
const val DISTANCE_EPSILON = 0.98999999999999999

fun ModelDefinition.getPoints(face: Int): Triple<
        Triple<Int, Int, Int>,
        Triple<Int, Int, Int>,
        Triple<Int, Int, Int>>
{
    val (v1, v2, v3) = getVertices(face)
    return Triple(
        Triple(getX(v1), getY(v1), getZ(v1)),
        Triple(getX(v2), getY(v2), getZ(v2)),
        Triple(getX(v3), getY(v3), getZ(v3)))
}

fun ModelDefinition.getVertices(face: Int) = Triple(getFaceVertexIndices1()[face], getFaceVertexIndices2()[face], getFaceVertexIndices3()[face])
fun ModelDefinition.getX(vertex: Int) = getVertexPositionsX()[vertex]
fun ModelDefinition.getY(vertex: Int) = getVertexPositionsY()[vertex]
fun ModelDefinition.getZ(vertex: Int) = getVertexPositionsZ()[vertex]
fun ModelDefinition.getColor(face: Int) = getFaceColors()[face].toInt()

class Bounds {
    var minY = 0
    var distance2D = 0
    var diagonal3DAboveOrigin = 0
    var maxRenderDepth = 0
    var minX = 999999
    var maxX = -999999
    var maxZ = -99999
    var minZ = 99999
    var maxY = 0
}

fun ModelDefinition.calculateBounds() : Bounds {
    return Bounds().apply {
        for (vertex in 0 until getVertexCount()) {
            val x = getX(vertex)
            val y = getY(vertex)
            val z = getZ(vertex)
            val distance2D = x * x + z * z
            if (distance2D > this.distance2D) this.distance2D = distance2D
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (z < minZ) minZ = z
            if (z > maxZ) maxZ = z
            if (-y > minY) minY = -y
            if (y > maxY) maxY = y
        }
        distance2D = (sqrt(distance2D.toDouble()) + DISTANCE_EPSILON).toInt()
        diagonal3DAboveOrigin = (sqrt(distance2D.toDouble() * distance2D + minY * minY) + DISTANCE_EPSILON).toInt()
        maxRenderDepth = (diagonal3DAboveOrigin + (sqrt((distance2D * distance2D + maxY * maxY).toDouble()) + DISTANCE_EPSILON).toInt())
    }
}

/**
 * Per face render types, as stored in the cache.
 *
 * Anything other than [RENDER_GOURAUD_TRIANGLE] disables per vertex interpolation for that face.
 */
const val RENDER_GOURAUD_TRIANGLE = 0
const val RENDER_FLAT_TRIANGLE = 1
const val RENDER_HIDDEN_TRIANGLE = 2
const val RENDER_UNLIT_TRIANGLE = 3

/**
 * The lighting the client uses for actors: npcs, players and spot animations.
 *
 * Their definitions offset these with their own `ambient` and `contrast` fields.
 */
const val ACTOR_AMBIENT = 64
const val ACTOR_CONTRAST = 850
const val ACTOR_LIGHT_X = -30
const val ACTOR_LIGHT_Y = -50
const val ACTOR_LIGHT_Z = -30

/**
 * The lighting the client uses for scenery: objects, items and interface models.
 *
 * The light comes in at a shallower angle than [ACTOR_LIGHT_Y], which is what keeps the tops of
 * world objects from washing out.
 */
const val SCENERY_AMBIENT = 64
const val SCENERY_CONTRAST = 768
const val SCENERY_LIGHT_X = -50
const val SCENERY_LIGHT_Y = -10
const val SCENERY_LIGHT_Z = -50

/**
 * The colour the client gives faces it lights but does not shade.
 */
private const val UNLIT_COLOR = 128

/**
 * Whether the client discards this face before drawing.
 *
 * Faces are hidden either by carrying render type 2 or by being fully transparent, which the
 * client folds into the same case. Hiding is a property of the model itself, so it applies whether
 * or not the model is being shaded.
 */
fun ModelDefinition.isFaceHidden(face: Int): Boolean {
    val alpha = getFaceAlphas()?.get(face)?.toInt()
    if (alpha == -1)
        return true
    if (alpha == -2)
        return false
    val type = getFaceTypes()?.get(face)?.toInt() ?: RENDER_GOURAUD_TRIANGLE
    return type != RENDER_GOURAUD_TRIANGLE &&
        type != RENDER_FLAT_TRIANGLE &&
        type != RENDER_UNLIT_TRIANGLE
}

/**
 * The lit colour of every face corner, as packed 16 bit HSL values.
 *
 * Faces that the client shades flat (or does not shade at all) carry the same colour in all
 * three corners, so consumers never have to branch on the render type.
 */
class FaceShading(
    val corner1: IntArray,
    val corner2: IntArray,
    val corner3: IntArray
) {
    fun isFlat(face: Int) = corner1[face] == corner2[face] && corner2[face] == corner3[face]
}

/**
 * Lights this model the way the client does.
 *
 * Vertex normals are the summed face normals of every adjoining Gouraud face, which is what makes
 * curved surfaces read as smooth; flat faces use their own face normal instead. The resulting
 * brightness only scales the lightness component of the face colour, leaving hue and saturation
 * untouched.
 *
 * Lighting is baked once rather than re-evaluated per animation frame, matching the client.
 */
fun ModelDefinition.light(
    ambient: Int = ACTOR_AMBIENT,
    contrast: Int = ACTOR_CONTRAST,
    lightX: Int = ACTOR_LIGHT_X,
    lightY: Int = ACTOR_LIGHT_Y,
    lightZ: Int = ACTOR_LIGHT_Z,
    faceColors: IntArray? = null
): FaceShading {

    val faceCount = getFaceCount()
    val corner1 = IntArray(faceCount)
    val corner2 = IntArray(faceCount)
    val corner3 = IntArray(faceCount)

    val lightMagnitude = sqrt((lightX * lightX + lightY * lightY + lightZ * lightZ).toDouble()).toInt()
    val attenuation = contrast * lightMagnitude shr 8
    // Guards against a zero light vector, which would otherwise divide by zero below.
    val safeAttenuation = if (attenuation == 0) 1 else attenuation

    val (vertexNormals, faceNormals) = calculateNormals()
    val faceTypes = getFaceTypes()
    val faceAlphas = getFaceAlphas()
    val colors = faceColors ?: IntArray(faceCount) { getFaceColors()[it].toInt() and 0xFFFF }

    for (face in 0 until faceCount) {

        val color = colors[face] and 0xFFFF
        var type = faceTypes?.get(face)?.toInt() ?: RENDER_GOURAUD_TRIANGLE
        // Alpha overrides the render type before anything is lit: 255 hides the face outright and
        // 254 leaves it unshaded.
        when (faceAlphas?.get(face)?.toInt()) {
            -1 -> type = RENDER_HIDDEN_TRIANGLE
            -2 -> type = RENDER_UNLIT_TRIANGLE
        }

        when (type) {
            RENDER_UNLIT_TRIANGLE -> {
                corner1[face] = UNLIT_COLOR
                corner2[face] = UNLIT_COLOR
                corner3[face] = UNLIT_COLOR
            }
            RENDER_GOURAUD_TRIANGLE -> {
                val (v1, v2, v3) = getVertices(face)
                corner1[face] = shade(color, ambient + vertexBrightness(vertexNormals[v1], lightX, lightY, lightZ, safeAttenuation))
                corner2[face] = shade(color, ambient + vertexBrightness(vertexNormals[v2], lightX, lightY, lightZ, safeAttenuation))
                corner3[face] = shade(color, ambient + vertexBrightness(vertexNormals[v3], lightX, lightY, lightZ, safeAttenuation))
            }
            RENDER_FLAT_TRIANGLE -> {
                val normal = faceNormals[face] ?: VertexNormal()
                // Flat faces are lit at 1.5x the attenuation of a Gouraud face.
                val divisor = safeAttenuation / 2 + safeAttenuation
                val brightness = ambient +
                    (lightX * normal.x + lightY * normal.y + lightZ * normal.z) / divisor
                val lit = shade(color, brightness)
                corner1[face] = lit
                corner2[face] = lit
                corner3[face] = lit
            }
            else -> {
                corner1[face] = color
                corner2[face] = color
                corner3[face] = color
            }
        }
    }

    return FaceShading(corner1, corner2, corner3)
}

/**
 * Applies a brightness to a packed HSL colour by scaling its lightness component only.
 *
 * The lightness is never allowed to reach 0 or 127, because those are pure black and pure white
 * and would lose the hue entirely.
 */
private fun shade(hsl: Int, brightness: Int): Int {
    var lightness = brightness * (hsl and 127) shr 7
    if (lightness < 2)
        lightness = 2
    else if (lightness > 126)
        lightness = 126
    return (hsl and 65408) + lightness
}

private fun vertexBrightness(normal: VertexNormal, lightX: Int, lightY: Int, lightZ: Int, attenuation: Int): Int {
    if (normal.magnitude == 0)
        return 0
    return (lightX * normal.x + lightY * normal.y + lightZ * normal.z) / (attenuation * normal.magnitude)
}

/**
 * Computes the vertex and face normals of this model.
 *
 * Only Gouraud faces contribute to vertex normals; flat faces get a normal of their own so that
 * they keep a hard edge against their neighbours.
 *
 * Unlike [ModelDefinition.computeNormals] this does not cache, so it always reflects the current
 * vertex positions.
 */
private fun ModelDefinition.calculateNormals(): Pair<Array<VertexNormal>, Array<VertexNormal?>> {

    val vertexNormals = Array(getVertexCount()) { VertexNormal() }
    val faceNormals = arrayOfNulls<VertexNormal>(getFaceCount())
    val faceTypes = getFaceTypes()

    for (face in 0 until getFaceCount()) {

        val (v1, v2, v3) = getVertices(face)
        val ax = getX(v2) - getX(v1)
        val ay = getY(v2) - getY(v1)
        val az = getZ(v2) - getZ(v1)
        val bx = getX(v3) - getX(v1)
        val by = getY(v3) - getY(v1)
        val bz = getZ(v3) - getZ(v1)

        var nx = ay * bz - by * az
        var ny = az * bx - bz * ax
        var nz = ax * by - bx * ay

        // Keeps the following squares inside the range of a 32 bit integer.
        while (nx > MAX_VIEW_DISTANCE || ny > MAX_VIEW_DISTANCE || nz > MAX_VIEW_DISTANCE ||
            nx < -MAX_VIEW_DISTANCE || ny < -MAX_VIEW_DISTANCE || nz < -MAX_VIEW_DISTANCE
        ) {
            nx = nx shr 1
            ny = ny shr 1
            nz = nz shr 1
        }

        var length = sqrt((nx * nx + ny * ny + nz * nz).toDouble()).toInt()
        if (length <= 0)
            length = 1
        nx = nx * 256 / length
        ny = ny * 256 / length
        nz = nz * 256 / length

        when (faceTypes?.get(face)?.toInt() ?: RENDER_GOURAUD_TRIANGLE) {
            RENDER_GOURAUD_TRIANGLE -> {
                for (vertex in intArrayOf(v1, v2, v3)) {
                    val normal = vertexNormals[vertex]
                    normal.x += nx
                    normal.y += ny
                    normal.z += nz
                    normal.magnitude++
                }
            }
            RENDER_FLAT_TRIANGLE -> {
                faceNormals[face] = VertexNormal().apply {
                    x = nx
                    y = ny
                    z = nz
                    magnitude = 1
                }
            }
        }
    }

    return vertexNormals to faceNormals
}
