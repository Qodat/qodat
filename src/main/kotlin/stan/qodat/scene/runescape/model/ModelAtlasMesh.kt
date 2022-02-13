package stan.qodat.scene.runescape.model

import fxyz3d.geometry.Point3F
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.DepthTest
import javafx.scene.paint.Material
import javafx.scene.shape.CullFace
import javafx.scene.shape.DrawMode
import javafx.scene.shape.MeshView
import qodat.cache.definition.ModelDefinition
import stan.qodat.scene.paint.AtlasMaterial
import stan.qodat.util.ModelUtil
import stan.qodat.util.setAndBind

/**
 * A [ModelAtlasMesh] is a mesh that contains all model information.
 *
 * Out of the box JavaFX does not allow us to color individual triangles in the same mesh.
 * This implementation circumvents this issue by mapping all unique colors of the model to a texture.
 *
 * For each face the UV coordinate of the face color is stores as that face's tex coords.
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/09/2019
 */
class ModelAtlasMesh(private val model: Model, private val faceList: List<Int>? = null) : ModelMesh() {

    private lateinit var meshView: MeshView

    val visibleProperty = SimpleBooleanProperty()
    val drawModeProperty = SimpleObjectProperty<DrawMode>()
    val cullFaceProperty = SimpleObjectProperty<CullFace>()
    val materialProperty = SimpleObjectProperty<Material>()
    val depthTestProperty = SimpleObjectProperty<DepthTest>()

    private var densityFunction: (Point3F) -> Double = {0.0}
    private var min = 0.0
    private var max = 1.0

    init {
        visibleProperty.setAndBind(model.visibleProperty)
        drawModeProperty.setAndBind(model.drawModeProperty)
        cullFaceProperty.setAndBind(model.cullFaceProperty)
        depthTestProperty.setAndBind(model.depthTestProperty)
    }

    override fun getSceneNode(): MeshView {
        if (!this::meshView.isInitialized){
            buildMesh()
            meshView = MeshView(this)
            meshView.isPickOnBounds = false
            meshView.isMouseTransparent = true
            meshView.visibleProperty().setAndBind(visibleProperty, true)
            meshView.cullFaceProperty().setAndBind(cullFaceProperty, true)
            meshView.materialProperty().setAndBind(materialProperty, true)
            meshView.drawModeProperty().setAndBind(drawModeProperty, true)
            meshView.depthTestProperty().setAndBind(depthTestProperty, true)
        }
        return meshView
    }

    /**
     * Build this mesh from the specified [model].
     */
    private fun buildMesh() {

        val definition = model.modelDefinition
        val atlasMaterial = createAtlas(definition)

        materialProperty.set(atlasMaterial)

        val faceIterator = faceList?:(0 until definition.getFaceCount())
        for (face in faceIterator) {

            val vertexIndex1 = definition.getFaceVertexIndices1()[face].let {
                addVertex(
                        it,
                        definition.getVertexPositionsX()[it],
                        definition.getVertexPositionsY()[it],
                        definition.getVertexPositionsZ()[it]
                )
            }
            val vertexIndex2 = definition.getFaceVertexIndices2()[face].let {
                addVertex(
                        it,
                        definition.getVertexPositionsX()[it],
                        definition.getVertexPositionsY()[it],
                        definition.getVertexPositionsZ()[it]
                )
            }
            val vertexIndex3 = definition.getFaceVertexIndices3()[face].let {
                addVertex(
                        it,
                        definition.getVertexPositionsX()[it],
                        definition.getVertexPositionsY()[it],
                        definition.getVertexPositionsZ()[it]
                )
            }

            /*
            Compute UV coordinates of colors in the atlas.

            The U coordinate represents the centre x coordinate of the requested color in the image.
            The V coordinate represents the centre y coordinate which in this case is always 0.5f (half a pixel).
             */
            val texIndex = addUV(u = atlasMaterial.getU(face), v = 0.5F)

            faces.addAll(
                    vertexIndex1, texIndex,
                    vertexIndex2, texIndex,
                    vertexIndex3, texIndex
            )
            // (1 shl (definition.getFacePriorities()?.get(face)?.toInt()?:definition.getPriority().toInt()))
            faceSmoothingGroups.addAll(0)
        }
    }

    fun rebuildAtlas(){
        val atlasMaterial = createAtlas(model.modelDefinition)
        materialProperty.set(atlasMaterial)
    }

    /**
     * Create a new [AtlasMaterial].
     */
    private fun createAtlas(definition: ModelDefinition) : AtlasMaterial{
        val atlas = AtlasMaterial()

        val faceRange = (0 until definition.getFaceCount())
        if (!faceRange.isEmpty()) {
            atlas.setColors(faceRange.map {
                ModelUtil.hsbToColor(
                    getColor(definition, it),
                    definition.getFaceAlphas()?.get(it)
                )
            }.toTypedArray())
        }
        return atlas
    }

    private fun getColor(definition: ModelDefinition, it: Int): Short {
        var color = definition.getFaceColors()[it]
        val findColor = model.findColor
        val replaceColor = model.replaceColor
        if (findColor != null && replaceColor != null) {
            assert(findColor.size == replaceColor.size)
            for (i in 0 until findColor.size){
                if (findColor[i] == color)
                    color = replaceColor[i]
            }
        }
        return color
    }
}