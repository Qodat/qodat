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
import stan.qodat.scene.runescape.getX
import stan.qodat.scene.runescape.getY
import stan.qodat.scene.runescape.getZ
import java.awt.Color
import java.io.PrintWriter

class ModelDefinitionObjExporter(private val model: ModelDefinition) {

    fun export(name: String, objWriter: PrintWriter, mtlWriter: PrintWriter) {

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


        val materials = HashSet<Material>()

        // Write material
        for (i in 0 until model.getFaceCount()) {

            val material = model.getMaterial(i)
            materials.add(material)
        }

        for (i in 0 until model.getFaceCount()) {
            val x = model.getFaceVertexIndices1()[i] + 1
            val y = model.getFaceVertexIndices2()[i] + 1
            val z = model.getFaceVertexIndices3()[i] + 1
            val material = model.getMaterial(i)
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