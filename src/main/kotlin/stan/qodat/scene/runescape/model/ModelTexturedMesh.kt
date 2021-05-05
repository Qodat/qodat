package stan.qodat.scene.runescape.model

import fxyz3d.geometry.Face3
import fxyz3d.scene.paint.Palette
import fxyz3d.shapes.primitives.TexturedMesh
import fxyz3d.shapes.primitives.helper.TriangleMeshHelper
import javafx.scene.shape.MeshView
import stan.qodat.util.ModelUtil

class ModelTexturedMesh(private val model: Model) : TexturedMesh(), ModelSkin {

    init {
        sectionType = TriangleMeshHelper.SectionType.TRIANGLE
        textureType = TriangleMeshHelper.TextureType.COLORED_VERTICES_3D
        helper.sectionType = sectionType
        updateMesh()
    }

    override fun updateMesh() {
        val definition = model.modelDefinition

        listVertices.clear()
        listTextures.clear()
        listFaces.clear()

        for (face in 0 until definition.getFaceCount()) {
            val (p1, p2, p3) = model.getPoints(face)
            listVertices.add(p1)
            listVertices.add(p2)
            listVertices.add(p3)
            val (v1, v2, v3) = model.getVertices(face)
            listFaces.add(Face3(v1, v2, v3))
            if (definition.getFaceTextures() != null) {
                val (v4, v5, v6) = model.getTextureVertices(face)
                listTextures.add(Face3(v4, v5, v6))
            }
        }

        val palette = Palette.ListColorPalette(MutableList(definition.getFaceCount()) {
            ModelUtil.rs2hsbToColor(
                definition.getFaceColors()[it],
                definition.getFaceAlphas()?.get(it))
        })
        setTextureModeVertices3D(palette, density)

        setMesh(null)
        mesh = createMesh()
        setMesh(mesh)
    }

    override fun updatePoints(skeleton: ModelSkeleton) {
        TODO("Not yet implemented")
    }

    override fun getSceneNode(): MeshView {
        return this
    }
}