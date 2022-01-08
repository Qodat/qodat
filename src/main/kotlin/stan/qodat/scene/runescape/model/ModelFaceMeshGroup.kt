package stan.qodat.scene.runescape.model

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.Group
import javafx.scene.paint.Material
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.CullFace
import javafx.scene.shape.DrawMode
import javafx.scene.shape.MeshView
import stan.qodat.cache.definition.ModelDefinition
import stan.qodat.util.ModelUtil
import stan.qodat.util.setAndBind

/**
 * This class represents a [ModelSkin] where each triangle in the specified [ModelDefinition]
 * corresponds to one [ModelFaceMesh] child in this [faceMeshGroup].
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/09/2019
 * @version 1.0
 */
class ModelFaceMeshGroup(private val model: Model) : ModelSkin {

    private lateinit var faceMeshGroup: Group
    private lateinit var materials: List<Material>

    val visibleProperty = SimpleBooleanProperty()
    val drawModeProperty = SimpleObjectProperty<DrawMode>()
    val cullFaceProperty = SimpleObjectProperty<CullFace>()
    val editableProperty = SimpleObjectProperty<(ModelFaceMesh.EditContext.() -> Unit)?>(null)

    init {
        visibleProperty.setAndBind(model.visibleProperty)
        drawModeProperty.setAndBind(model.drawModeProperty)
        cullFaceProperty.setAndBind(model.cullFaceProperty)
        editableProperty.setAndBind(model.editProperty)
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
            val faceMeshSceneNodes = faceMeshes.map {
                it.getSceneNode()
            }
            faceMeshGroup.children.addAll(faceMeshSceneNodes)
        }
        return faceMeshGroup
    }

    private fun createMeshes(definition: ModelDefinition): MutableList<ModelFaceMesh> {
        return MutableList(definition.getFaceCount()) { face ->

            val material = materials[face]
            val mesh = ModelFaceMesh(face, material)

            mesh.visibleProperty.setAndBind(visibleProperty)
            mesh.cullFaceProperty.setAndBind(cullFaceProperty)
            mesh.drawModeProperty.setAndBind(drawModeProperty)
            mesh.editableProperty.setAndBind(editableProperty)

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

            val u = definition.getFaceTextureUCoordinates()?.get(face) ?: floatArrayOf(-1f, -1f, -1f)
            val v = definition.getFaceTextureVCoordinates()?.get(face) ?: floatArrayOf(-1f, -1f, -1f)

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

    private fun createMaterials(definition: ModelDefinition) : List<Material> {
        val colorMap = HashMap<Short, PhongMaterial>()
        return MutableList<Material>(definition.getFaceCount()) {
            definition.getFaceColors()[it].let { color ->
                colorMap.getOrPut(color) {
                    PhongMaterial(ModelUtil.hsbToColor(color, definition.getFaceAlphas()?.get(it)))
                }
            }
        }
    }
}