package mqo;

import qodat.cache.definition.ModelDefinition
import qodat.cache.models.RS2Model
import java.awt.Color
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt


/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   2019-07-18
 * @version 1.0
 */
@Suppress("unused")
class MQOImporter {

    fun load(file: File) : ModelDefinition {

        val reader = file.bufferedReader()
        val parser = MQOParser(reader).also { it.parse() }
        val figure = parser.convertFigure()

        var mesh = figure[0]

        val id = 0
        val priority = 0.toByte()
        val verticesCount =  mesh.vertexCount
        val vertexPositionsX = mesh.verticesX.map { it.toInt() }.toIntArray()
        val vertexPositionsY = mesh.verticesY.map { it.toInt() }.toIntArray()
        val vertexPositionsZ = mesh.verticesZ.map { it.toInt() }.toIntArray()
        val vertexSkins = IntArray(mesh.vertexCount)
        val faceCount =  mesh.faceCount
        val faceVertexIndices1 = mesh.triangleViewPortX
        val faceVertexIndices2 = mesh.triangleViewPortY
        val faceVertexIndices3 = mesh.triangleViewPortZ
        val faceSkins = IntArray(faceCount)
        val faceColors = ShortArray(faceCount) {
            val material = figure.getMaterial(mesh.triangleMaterials[it])
            rgbToHSL(material.r, material.g,material.b).toShort()
        }
        val faceAlphas = ByteArray(faceCount) {
            (figure.getMaterial(mesh.triangleMaterials[it]).a * 255.0).roundToInt().toByte()
        }
        val faceRenderPriorities = ByteArray(faceCount)

        for(i in 0 until figure.getMeshCount()){
            mesh = figure[i]
            when {
                mesh.name.startsWith("PRI") -> for((face, mat) in mesh.triangleMaterials.withIndex()) faceRenderPriorities[face] = mat.toByte()
                mesh.name.startsWith("VSKIN") -> for((vertex, weight) in mesh.verticesWeights)
                    vertexSkins[vertex] += weight
                mesh.name.startsWith("TSKIN") -> for((face, mat) in mesh.triangleMaterials.withIndex()) faceSkins[face] = mat
            }
        }

        for((wi, weight) in vertexSkins.withIndex()){
            if(weight > 254)
                vertexSkins[wi] = 254
            if(weight < 0)
                vertexSkins[wi] = 0
        }

        val definition = RS2Model()
        definition.setId(id.toString())
        definition.setPriority(priority)
        definition.setVertexCount(verticesCount)
        definition.setVertexPositionsX(vertexPositionsX)
        definition.setVertexPositionsY(vertexPositionsY)
        definition.setVertexPositionsZ(vertexPositionsZ)
        definition.setVertexSkins(vertexSkins)
        definition.setFaceCount(faceCount)
        definition.setFaceVertexIndices1(faceVertexIndices1)
        definition.setFaceVertexIndices2(faceVertexIndices2)
        definition.setFaceVertexIndices3(faceVertexIndices3)
        definition.setFaceSkins(faceSkins)
        definition.setFaceColors(faceColors)
        definition.setFaceAlphas(if(faceAlphas.any { it != 255.toByte() }) faceAlphas else null)
        definition.setFaceRenderPriorities(faceRenderPriorities)
        return definition
    }

    companion object {

        fun rgbToHSB(red: Int, green: Int, blue: Int): Int {

            val hsb = Color.RGBtoHSB(red, green, blue, null)

            val hue = (hsb[0] * 63).toInt()
            val saturation = (hsb[1] * 7).toInt()
            val brightness = (hsb[2] * 127).toInt()

            return (hue shl 10) + (saturation shl 7) + brightness

        }
        fun rgbToHSL(r: Double, g: Double, b: Double) : Int {
            var v: Double
            var m: Double
            var vm: Double
            var h: Double
            var s: Double
            var l: Double
            var r2: Double
            var g2: Double
            var b2: Double

            h = 0.0
            s = 0.0
            l = 0.0

            v = max(r, g)
            v = max(v, b)
            m = min(r, g)
            m = min(m, b)
            l = (m+v) / 2.0
            if(l <= 0.0){
                l = 0.0
            }
            vm = v - m
            s = vm
            if(s > 0.0){
                s /= if(l <= 0.5) (v + m) else (2.0 - v - m)
            } else {
                s = 0.0
            }
            r2 = (v - r) / vm
            g2 = (v - g) / vm
            b2 = (v - b) / vm
            h = when {
                r == v -> if (g == m) 5.0 + b2 else 1.0 - g2
                g == v -> if (b == m) 1.0 + r2 else 3.0 - b2
                else -> if (r == m) 3.0 + g2 else 5.0 - r2
            }
            h /= 6.0;
            var l2 = l * 127.0
            var s2 = s * 7.0
            var h2 = h * 63.0
            if(l2 < 0.0)
                l2 = 0.0
            if(s2 < 0.0)
                s2 = 0.0
            if(h2 < 0.0)
                h2 = 0.0
            val truncatedL = l2.toInt()
            val truncatedS = s2.toInt()
            val truncatedH = h2.toInt()
            var value = (truncatedL + (truncatedS * 128.0) + (truncatedH * 1024)).toInt()

            if(value > 65535)
                value = 65535
            if(value < 0)
                value = 0

            return value
        }
    }
}