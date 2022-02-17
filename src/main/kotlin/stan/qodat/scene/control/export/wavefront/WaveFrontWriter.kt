package stan.qodat.scene.control.export.wavefront

import stan.qodat.scene.runescape.model.Model
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.file.Path

class WaveFrontWriter(private val saveDir: Path) {

    fun writeMtlFile(materials: Set<WaveFrontMaterial>, fileNameWithoutExtension: String) {
        PrintWriter(FileWriter("$saveDir/${fileNameWithoutExtension.replace(" ", "_")}.mtl")).use { mtlFileWriter ->
            for ((i, material) in materials.withIndex()) {
                mtlFileWriter.println("newmtl m$i")
                material.encodeMtl(mtlFileWriter, saveDir)
            }
        }
    }

    fun writeObjFile(
        model: Model,
        materials: Set<WaveFrontMaterial>,
        computeTextureUVCoordinate: Boolean = false,
        mtlFileNameWithoutExtension: String,
        objFileNameWithoutExtension: String
    ) {
        val modelDefinition = model.modelDefinition


        if (computeTextureUVCoordinate)
            modelDefinition.computeTextureUVCoordinates()

        val mtlFileName = mtlFileNameWithoutExtension
        val objFormattedName = objFileNameWithoutExtension
        val objOutFile = saveDir.resolve("$objFormattedName.obj").toFile()

        PrintWriter(FileWriter(objOutFile)).use { objFileWriter ->

            objFileWriter.println("mtllib $mtlFileName.mtl")
            objFileWriter.println("o $objFormattedName")

            for (i in 0 until model.getVertexCount()) {
                val (x, y, z) = model.getXYZ(i)
                objFileWriter.println("v $x ${(y * -1)} ${(z * -1)}")
            }

            if (modelDefinition.getFaceTextures() != null) {
                val u = modelDefinition.getFaceTextureUCoordinates()!!
                val v = modelDefinition.getFaceTextureVCoordinates()!!
                for (i in 0 until model.getFaceCount()) {
                    objFileWriter.println("vt " + u[i][0] + " " + v[i][0])
                    objFileWriter.println("vt " + u[i][1] + " " + v[i][1])
                    objFileWriter.println("vt " + u[i][2] + " " + v[i][2])
                }
            }

            for (normal in  model.calculateVertexNormals()) {
                objFileWriter.println("vn " + normal.x + " " + normal.y + " " + normal.z)
            }

            for (face in 0 until model.getFaceCount()) {
                val (v1, v2, v3) = model.getVertices(face)
                val material = modelDefinition.getFaceMaterial(face)
                val materialIndex = materials.indexOf(material)
                objFileWriter.println("usemtl m$materialIndex")
                material.encodeObj(objFileWriter, face, v1 + 1, v2 + 1, v3 + 1)
                objFileWriter.println("")
            }
        }
    }
}
