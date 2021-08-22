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

import stan.qodat.cache.definition.ModelDefinition
import stan.qodat.cache.impl.oldschool.OldschoolCacheRuneLite
import stan.qodat.scene.runescape.getX
import stan.qodat.scene.runescape.getY
import stan.qodat.scene.runescape.getZ
import java.awt.Color
import java.io.PrintWriter

class ObjExporter(private val model: ModelDefinition) {
    fun export(name: String, objWriter: PrintWriter, mtlWriter: PrintWriter) {
        val textureManager = OldschoolCacheRuneLite.textureManager
        model.computeNormals()
        model.computeTextureUVCoordinates()
        objWriter.println("mtllib $name.mtl")
        objWriter.println("o runescapemodel")
        for (i in 0 until model.getVertexCount()) {

            objWriter.println(
                "v ${model.getX(i)} ${model.getY(i) * -1} ${model.getZ(i) * -1}"
            )
        }
        if (model.getFaceTextures() != null) {
            val u = model.getFaceTextureUCoordinates()!!
            val v = model.getFaceTextureVCoordinates()!!
            for (i in 0 until model.getFaceCount()) {
                objWriter.println("vt " + u[i][0] + " " + v[i][0])
                objWriter.println("vt " + u[i][1] + " " + v[i][1])
                objWriter.println("vt " + u[i][2] + " " + v[i][2])
            }
        }
        for (normal in model.getVertexNormals()) {
            objWriter.println("vn " + normal.x + " " + normal.y + " " + normal.z)
        }
        for (i in 0 until model.getFaceCount()) {
            val x = model.getFaceVertexIndices1()[i] + 1
            val y = model.getFaceVertexIndices2()[i] + 1
            val z = model.getFaceVertexIndices3()[i] + 1
            objWriter.println("usemtl m$i")
            if (model.getFaceTextures() != null) {
                objWriter.println(
                    "f "
                            + x + "/" + (i * 3 + 1) + " "
                            + y + "/" + (i * 3 + 2) + " "
                            + z + "/" + (i * 3 + 3)
                )
            } else {
                objWriter.println("f $x $y $z")
            }
            objWriter.println("")
        }

        // Write material
        for (i in 0 until model.getFaceCount()) {
            val textureId = model.getFaceTextures()?.get(i)?:-1
            mtlWriter.println("newmtl m$i")
            if (textureId.toInt() == -1) {
                val color = rs2hsbToColor(
                    model.getFaceColors()[i].toInt()
                )
                val r = color.red / 255.0
                val g = color.green / 255.0
                val b = color.blue / 255.0
                mtlWriter.println("Kd $r $g $b")
            } else {
                val texture = textureManager.findTexture(textureId.toInt())!!
                mtlWriter.println("map_Kd sprite/" + texture.fileIds[0] + "-0.png")
            }
            val alpha = model.getFaceAlphas()?.get(i)?.let { it.toInt() and 0xFF }?:0
            if (alpha != 0) {
                mtlWriter.println("d " + alpha / 255.0)
            }
        }
    }

    companion object {
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
    }
}