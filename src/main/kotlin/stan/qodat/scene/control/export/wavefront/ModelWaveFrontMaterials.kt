package stan.qodat.scene.control.export.wavefront

import stan.qodat.scene.runescape.model.Model
import java.io.PrintWriter

class ModelWaveFrontMaterials(val name: String, model: Model) {

    val materials = HashSet<WaveFrontMaterial>()

    init {

        // Write material
        for (i in 0 until model.getFaceCount()) {

            val material = model.modelDefinition.getFaceMaterial(i)
            materials.add(material)
        }
    }

    fun write(mtlWriter: PrintWriter){
        for ((i, material) in materials.withIndex()) {
            mtlWriter.println("newmtl m$i")
            material.encode(mtlWriter)
        }
    }

    fun indexOf(material: WaveFrontMaterial.Color) = materials.indexOf(material)
    fun withIndex() = materials.withIndex()
}