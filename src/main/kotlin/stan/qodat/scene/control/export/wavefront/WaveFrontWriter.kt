package stan.qodat.scene.control.export.wavefront

import stan.qodat.scene.runescape.model.Model
import java.io.FileWriter
import java.io.PrintWriter
import java.nio.file.Path

class WaveFrontWriter(private val saveDir: Path) {

    fun writeMtlFile(materials: Set<WaveFrontMaterial>, fileNameWithoutExtension: String) {
        PrintWriter(FileWriter("$saveDir/$fileNameWithoutExtension.mtl")).use { mtlFileWriter ->
            for ((i, material) in materials.withIndex()) {
                mtlFileWriter.println("newmtl m$i")
                material.encode(mtlFileWriter)
            }
        }
    }

    fun writeObjFile(
        model: Model,
        materials: Set<WaveFrontMaterial>,
        computeNormals: Boolean = true,
        computeTextureUVCoordinate: Boolean = true,
        mtlFileNameWithoutExtension: String,
        objFileNameWithoutExtension: String
    ) {
        val modelDefinition = model.modelDefinition

        if (computeNormals)
            modelDefinition.computeNormals()
        if (computeTextureUVCoordinate)
            modelDefinition.computeTextureUVCoordinates()

        val outFile = saveDir.resolve("$objFileNameWithoutExtension.obj").toFile()

        PrintWriter(FileWriter(outFile)).use {  objFileWriter ->

            objFileWriter.println("mtllib $mtlFileNameWithoutExtension.mtl")
            objFileWriter.println("o $objFileNameWithoutExtension")

            for (i in 0 until model.getVertexCount()) {
                val (x, y, z) = model.getXYZ(i)
                objFileWriter.println("v ${x.div(SCALE_FACTOR)} ${(y * -1).div(SCALE_FACTOR)} ${(z * -1).div(SCALE_FACTOR)}")
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

            for (normal in modelDefinition.getVertexNormals()) {
                objFileWriter.println("vn " + normal.x + " " + normal.y + " " + normal.z)
            }

            for (face in 0 until model.getFaceCount()) {
                val (v1, v2, v3) = model.getVertices(face)
                val material = modelDefinition.getFaceMaterial(face)
                val materialIndex = materials.indexOf(material)
                objFileWriter.println("usemtl m$materialIndex")
                objFileWriter.println("f ${v1 + 1} ${v2 + 1} ${v3 + 1}")
                objFileWriter.println("")
            }
        }
    }

    companion object {

        private const val SCALE_FACTOR = 1.0
    }
}
