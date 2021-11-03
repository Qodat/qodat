package stan.qodat.scene.runescape

import net.runelite.cache.models.VertexNormal
import stan.qodat.cache.definition.ModelDefinition
import kotlin.math.absoluteValue
import kotlin.math.sqrt

const val MAX_VIEW_DISTANCE = 8192
const val DISTANCE_EPSILON = 0.98999999999999999

private fun VertexNormal.add(dx: Int, dy: Int, dz: Int) {
    x += dx
    y += dy
    z += dz
    magnitude++
}

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

const val RENDER_SHADED_TRIANGLE = 0
const val RENDER_FLAT_TRIANGLE = 1
const val RENDER_TEXTURED_TRIANGLE = 2

fun ModelDefinition.calculateFaceColors(brightnessOffset: Int, shadowModifier: Int, sizeX: Int, sizeY: Int, sizeZ: Int, shade: Boolean) : Triple<IntArray, IntArray, IntArray> {
    val area = sqrt((sizeX * sizeX + sizeY * sizeY + sizeZ * sizeZ).toDouble()).toInt()
    val shadow = shadowModifier * area shr 8 // shr 8 means >> 8

    val normals = Array(getVertexCount()) { VertexNormal() }
    val faceCount = getFaceCount()
    val faceColors1 = IntArray(faceCount)
    val faceColors2 = IntArray(faceCount)
    val faceColors3 = IntArray(faceCount)

    for (face in 0 until faceCount) {
        val (v1, v2, v3) = getVertices(face)
        val dx1 = getX(v2) - getX(v1)
        val dy1 = getY(v2) - getY(v1)
        val dz1 = getZ(v2) - getZ(v1)
        val dx2 = getX(v3) - getX(v1)
        val dy2 = getY(v3) - getY(v1)
        val dz2 = getZ(v3) - getZ(v1)
        var d1: Int = dy1 * dz2 - dy2 * dz1
        var d2: Int = dz1 * dx2 - dz2 * dx1
        var d3: Int = dx1 * dy2 - dx2 * dy1
        while (d1 > MAX_VIEW_DISTANCE || d2 > MAX_VIEW_DISTANCE || d3 > MAX_VIEW_DISTANCE
            || d1 < -MAX_VIEW_DISTANCE || d2 < -MAX_VIEW_DISTANCE || d3 < -MAX_VIEW_DISTANCE
        ) {
            d1 = d1 shr 1 // shr 1 means >> 1
            d2 = d2 shr 1
            d3 = d3 shr 1
        }
        var distance = sqrt((d1 * d1 + d2 * d2 + d3 * d3).toDouble()).toInt()
        if (distance <= 0) distance = 1
        d1 = d1 * 256 / distance
        d2 = d2 * 256 / distance
        d3 = d3 * 256 / distance
        // testures are always -1 at the moment
        val texture = getFaceTextures()?.get(face)?.toInt()?:-1
        var type = getFaceTypes()?.get(face)?.toInt()?:0
        /*
       the `type and 1 == 0` part functions as a `type % 2 == 0` check.
       TODO: find out if type is ever more than 3 (this check can be replaced with just `type == RENDER_TEXTURED_TRIANGLE` instead.
        */
        if (type == RENDER_SHADED_TRIANGLE || type and 1 == 0) {
            normals[v1].add(d1, d2, d3)
            normals[v2].add(d1, d2, d3)
            normals[v3].add(d1, d2, d3)
            continue
        }
        if (texture != -1)
            type = RENDER_TEXTURED_TRIANGLE
        faceColors1[face] = applyLighting(
            type = type,
            color = getColor(face),
            brightness = brightnessOffset + (sizeX * d1 + sizeY * d2 + sizeZ * d3) / (shadow + shadow / 2)
        )
    }
    if (shade)
        doShading(normals, faceColors1, faceColors2, faceColors3, brightnessOffset, shadow, sizeX, sizeY, sizeZ)
    return Triple(faceColors1, faceColors2, faceColors3)
}

fun applyLighting(type: Int, color: Int, brightness: Int): Int {
    if (color == 65535) return 0
    if (type and 2 == 2) {
        return when {
            brightness < 0 -> 127
            brightness > 127 -> 0
            else -> 127 - brightness
        }
    }
    var colorBrightness = brightness * (color and 127) shr 7
    if (colorBrightness < 2)
        colorBrightness = 2
    else if (colorBrightness > 126)
        colorBrightness = 126
    return (color and 65408) + colorBrightness
}

fun ModelDefinition.doShading(normals: Array<VertexNormal>, faceColors1: IntArray, faceColors2: IntArray, faceColors3: IntArray, brightnessOffset: Int, shadow: Int, sizeX: Int, sizeY: Int, sizeZ: Int) {
    for (face in 0 until getFaceCount()) {
        var skip = false
        val type = if (getFaceTextures()?.get(face)?.toInt()?:-1 != -1)
            RENDER_TEXTURED_TRIANGLE
        else
            getFaceTypes()
                ?.get(face)?.toInt()?.also {
                    if(it and 1 != 0)
                        skip = true
                }
                ?:RENDER_FLAT_TRIANGLE
        if (skip)
            continue
        val hsl = getFaceColors()[face].toInt() and 0xFFFF
        val (v1, v2, v3) = getVertices(face)
        val n1 = normals[v1]
        val n2 = normals[v2]
        val n3 = normals[v3]
        faceColors1[face] = applyLighting(
            type = type,
            color = hsl,
            brightness = brightnessOffset + (sizeX * n1.x + sizeY * n1.y + sizeZ * n1.z) / (shadow * n1.magnitude))
        faceColors2[face] = applyLighting(
            type = type,
            color = hsl,
            brightness = brightnessOffset + (sizeX * n2.x + sizeY * n2.y + sizeZ * n2.z) / (shadow * n2.magnitude))
        faceColors3[face] = applyLighting(
            type = type,
            color = hsl,
            brightness = brightnessOffset + (sizeX * n3.x + sizeY * n3.y + sizeZ * n3.z) / (shadow * n3.magnitude))
    }
}

fun ModelDefinition.draw(shade: Boolean = false) {
    val (colors1, colors2, colors3) = calculateFaceColors(64, 850, -30, -50, -30, shade)
    for (face in 0 until getFaceCount())
        drawFace(face, colors1, colors2, colors3)
}

fun ModelDefinition.drawFace(face: Int, colors1: IntArray, colors2: IntArray, colors3: IntArray) {
    val type = getFaceTypes()?.get(face)?.toInt()?.let { it and 3 }?:0
    if (type == RENDER_SHADED_TRIANGLE) {
        val (v1, v2, v3) = getVertices(face)
        Rasterizer3D.drawGouraudTriangle(
            getX(v1), getX(v2), getX(v3),
            getY(v1), getY(v2), getY(v3),
            getZ(v1).absoluteValue.toFloat(), getZ(v2).absoluteValue.toFloat(), getZ(v3).absoluteValue.toFloat(),
            colors1[face], colors2[face], colors3[face]
        )
    }
}
