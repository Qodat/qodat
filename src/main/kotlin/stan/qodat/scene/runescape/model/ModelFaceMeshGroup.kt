package stan.qodat.scene.runescape.model

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.DepthTest
import javafx.scene.Group
import javafx.scene.paint.Material
import javafx.scene.shape.CullFace
import javafx.scene.shape.DrawMode
import javafx.scene.shape.MeshView
import qodat.cache.definition.ModelDefinition
import stan.qodat.cache.impl.displee.DispleeCache
import stan.qodat.scene.paint.AtlasMaterial
import stan.qodat.scene.paint.ColorMaterial
import stan.qodat.scene.paint.FaceTint
import stan.qodat.scene.runescape.isFaceHidden
import stan.qodat.scene.runescape.light
import stan.qodat.util.ModelUtil
import stan.qodat.util.getMaterial
import stan.qodat.util.setAndBind

/**
 * This class represents a [ModelSkin] where each triangle in the specified [ModelDefinition]
 * corresponds to one [ModelFaceMesh] child in this [faceMeshGroup].
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/09/2019
 * @version 1.0
 */
class ModelFaceMeshGroup(
    private val model: Model) : ModelSkin {

    private lateinit var faceMeshGroup: Group
    private lateinit var materials: List<Material>

    val visibleProperty = SimpleBooleanProperty()
    val drawModeProperty = SimpleObjectProperty<DrawMode>()
    val cullFaceProperty = SimpleObjectProperty<CullFace>()
    val editableProperty = SimpleObjectProperty<(ModelFaceMesh.EditContext.() -> Unit)?>(null)
    val depthTestProperty = SimpleObjectProperty<DepthTest>()

    init {
        visibleProperty.setAndBind(model.visibleProperty)
        drawModeProperty.setAndBind(model.drawModeProperty)
        cullFaceProperty.setAndBind(model.cullFaceProperty)
        editableProperty.setAndBind(model.editProperty)
        depthTestProperty.setAndBind(model.depthTestProperty)
    }

    override fun updatePoints(skeleton: ModelSkeleton) {
        val group = getSceneNode()
        for(child in group.children){
            if(child is MeshView){
                val mesh = child.mesh
                if (mesh is ModelSkin)
                    mesh.updatePoints(skeleton)
            }
        }
    }

    override fun getSceneNode(): Group {
        if (!this::faceMeshGroup.isInitialized){
            faceMeshGroup = Group()
            val definition = model.modelDefinition
            materials = createMaterials(definition)
            val faceMeshes = createMeshes(definition)
            val faceMeshSceneNodes = faceMeshes.filterNot {
                definition.isFaceHidden(it.face)
            }.map {
                it.getSceneNode()
            }
            faceMeshGroup.children.addAll(faceMeshSceneNodes)
        }
        return faceMeshGroup
    }

    private fun createMeshes(definition: ModelDefinition): MutableList<ModelFaceMesh> {
        definition.computeTextureUVCoordinates()
        return MutableList(definition.getFaceCount()) { face ->

            val material = materials[face]
            val shadedMaterial = material as? AtlasMaterial
            val mesh = ModelFaceMesh(face, material)

            mesh.visibleProperty.setAndBind(visibleProperty)
            mesh.cullFaceProperty.setAndBind(cullFaceProperty)
            mesh.drawModeProperty.setAndBind(drawModeProperty)
            mesh.editableProperty.setAndBind(editableProperty)
            mesh.depthTestProperty.setAndBind(depthTestProperty)

            val vertexIndex1 = definition.getFaceVertexIndices1()[face].let {
                mesh.addVertex(
                    it,
                    definition.getVertexPositionsX()[it],
                    definition.getVertexPositionsY()[it],
                    definition.getVertexPositionsZ()[it]
                )
            }
            val vertexIndex2 = definition.getFaceVertexIndices2()[face].let {
                mesh.addVertex(
                    it,
                    definition.getVertexPositionsX()[it],
                    definition.getVertexPositionsY()[it],
                    definition.getVertexPositionsZ()[it]
                )
            }
            val vertexIndex3 = definition.getFaceVertexIndices3()[face].let {
                mesh.addVertex(
                    it,
                    definition.getVertexPositionsX()[it],
                    definition.getVertexPositionsY()[it],
                    definition.getVertexPositionsZ()[it]
                )
            }

            val u: FloatArray
            val v: FloatArray
            if (shadedMaterial != null) {
                // The face carries a shading tile rather than a cache texture, so its UVs point
                // at the corners of that tile instead of into the texture.
                u = FloatArray(3) { shadedMaterial.getU(0, it) }
                v = FloatArray(3) { shadedMaterial.getV(0, it) }
            } else {
                u = definition.getFaceTextureUCoordinates()?.get(face) ?: floatArrayOf(-1f, -1f, -1f)
                v = definition.getFaceTextureVCoordinates()?.get(face) ?: floatArrayOf(-1f, -1f, -1f)
            }

            val texIndex1 = mesh.addUV(u[0], v[0])
            val texIndex2 = mesh.addUV(u[1], v[1])
            val texIndex3 = mesh.addUV(u[2], v[2])

            mesh.faces.addAll(
                vertexIndex1, texIndex1,
                vertexIndex2, texIndex2,
                vertexIndex3, texIndex3
            )
            mesh
        }
    }

    /**
     * Builds one material per face.
     *
     * Faces that carry a cache texture keep that texture; the rest get their lit colour, which for
     * a shaded model means a private one-face [AtlasMaterial] holding the Gouraud gradient.
     */
    private fun createMaterials(definition: ModelDefinition) : List<Material> {
        val shading = if (model.shadingProperty.get())
            definition.light()
        else
            null
        return MutableList(definition.getFaceCount()) { face ->
            val material = definition.getMaterial(face, DispleeCache)
            if (shading == null || material !is ColorMaterial)
                return@MutableList material.fxMaterial
            val alpha = model.getRenderFaceAlphas()?.get(face)
            AtlasMaterial().apply {
                setFaceTints(arrayOf(FaceTint(
                    ModelUtil.hsbToColor(shading.corner1[face], alpha),
                    ModelUtil.hsbToColor(shading.corner2[face], alpha),
                    ModelUtil.hsbToColor(shading.corner3[face], alpha)
                )))
            }
        }
    }
}