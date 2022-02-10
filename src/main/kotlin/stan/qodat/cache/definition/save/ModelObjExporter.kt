/*
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package stan.qodat.cache.definition.save

import qodat.cache.definition.ModelDefinition
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.scene.runescape.animation.AnimationFrame
import stan.qodat.scene.runescape.model.Model
import java.awt.Color
import java.io.PrintWriter

class ModelObjExporter(private val model: Model, private val animationFrame: AnimationFrame? = null) {

    fun export(name: String, objWriter: PrintWriter, mtlWriter: PrintWriter) {

        val textureManager = OldschoolCacheRuneLite.textureManager

        model.modelDefinition.computeNormals()
        model.modelDefinition.computeTextureUVCoordinates()

        if (animationFrame != null)
            model.animate(animationFrame)

        objWriter.println("mtllib $name.mtl")
        objWriter.println("o runescapemodel")

        for (i in 0 until model.getVertexCount()) {
            val (x, y, z) = model.getXYZ(i)
            objWriter.println("v $x ${y * -1} ${z * -1}")
        }

        if (model.modelDefinition.getFaceTextures() != null) {
            val u = model.modelDefinition.getFaceTextureUCoordinates()!!
            val v = model.modelDefinition.getFaceTextureVCoordinates()!!
            for (i in 0 until model.getFaceCount()) {
                objWriter.println("vt " + u[i][0] + " " + v[i][0])
                objWriter.println("vt " + u[i][1] + " " + v[i][1])
                objWriter.println("vt " + u[i][2] + " " + v[i][2])
            }
        }
        for (normal in model.calculateVertexNormals()) {
            objWriter.println("vn " + normal.x + " " + normal.y + " " + normal.z)
        }

        val materials = HashSet<Material>()

        // Write material
        for (i in 0 until model.getFaceCount()) {

            val material = model.modelDefinition.getMaterial(i)
            materials.add(material)
        }

        for (i in 0 until model.getFaceCount()) {
            val x = model.modelDefinition.getFaceVertexIndices1()[i] + 1
            val y = model.modelDefinition.getFaceVertexIndices2()[i] + 1
            val z = model.modelDefinition.getFaceVertexIndices3()[i] + 1
            val material = model.modelDefinition.getMaterial(i)
            val materialIndex = materials.indexOf(material)
            objWriter.println("usemtl m$materialIndex")
            objWriter.println("f $x $y $z")
            objWriter.println("")
        }



        for ((i, material) in materials.withIndex()) {
            mtlWriter.println("newmtl m$i")
            material.encode(mtlWriter)
        }
    }
}


sealed class Material {

    abstract fun encode(mtlWriter: PrintWriter)


    data class Color(private val r: Double, private val g: Double, private val b: Double, private val alpha: Double) : Material() {
        override fun encode(mtlWriter: PrintWriter) {
            mtlWriter.println("Kd $r $g $b")
            if (alpha > 0.0)
                mtlWriter.println("d $alpha")
        }
    }

}
fun ModelDefinition.getMaterial(i: Int): Material.Color {
    val color = rs2hsbToColor(
       getFaceColors()[i].toInt()
    )
    val r = color.red / 255.0
    val g = color.green / 255.0
    val b = color.blue / 255.0
    val a = getFaceAlphas()?.get(i)?.let { it.toInt() and 0xFF }?.div(255.0) ?: 0.0
    val material = Material.Color(r, g, b, a)
    return material
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