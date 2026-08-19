package stan.qodat.scene.runescape.model

import fxyz3d.geometry.Point3F
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import javafx.scene.DepthTest
import javafx.scene.Node
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.paint.Material
import javafx.scene.shape.CullFace
import javafx.scene.shape.DrawMode
import javafx.scene.shape.MeshView
import qodat.cache.definition.ModelDefinition
import stan.qodat.scene.paint.AtlasMaterial
import stan.qodat.scene.paint.FaceTint
import stan.qodat.scene.runescape.isFaceHidden
import stan.qodat.scene.runescape.light
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
    private val faceTexCoordOnsets = ArrayList<Int>()
    private val atlasFaces = ArrayList<Int>()

    val visibleProperty = SimpleBooleanProperty()
    val drawModeProperty = SimpleObjectProperty<DrawMode>()
    val cullFaceProperty = SimpleObjectProperty<CullFace>()
    val materialProperty = SimpleObjectProperty<Material>()
    val depthTestProperty = SimpleObjectProperty<DepthTest>()

    private var densityFunction: (Point3F) -> Double = {0.0}
    private var min = 0.0
    private var max = 1.0

    private val surfaceHoverHandler = EventHandler<MouseEvent> { event ->
        when (event.eventType) {
            MouseEvent.MOUSE_ENTERED, MouseEvent.MOUSE_MOVED -> {
                if (!model.viewModeProperty.get().isDiagnostic())
                    return@EventHandler
                val meshFace = event.pickResult?.intersectedFace ?: -1
                val face = atlasFaces.getOrNull(meshFace) ?: return@EventHandler
                val text = model.hoverTextForFace(face) ?: return@EventHandler
                ModelOverlayHover.show(event.source as Node, event, text)
            }
            MouseEvent.MOUSE_EXITED, MouseEvent.MOUSE_PRESSED -> ModelOverlayHover.hide()
        }
    }

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
            meshView.addEventHandler(MouseEvent.MOUSE_ENTERED, surfaceHoverHandler)
            meshView.addEventHandler(MouseEvent.MOUSE_MOVED, surfaceHoverHandler)
            meshView.addEventHandler(MouseEvent.MOUSE_EXITED, surfaceHoverHandler)
            meshView.addEventHandler(MouseEvent.MOUSE_PRESSED, surfaceHoverHandler)
            syncSurfaceHover()
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

            if (definition.isFaceHidden(face))
                continue

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
            val texIndex1 = addUV(atlasMaterial.getU(face, 0), atlasMaterial.getV(face, 0))
            val texIndex2 = addUV(atlasMaterial.getU(face, 1), atlasMaterial.getV(face, 1))
            val texIndex3 = addUV(atlasMaterial.getU(face, 2), atlasMaterial.getV(face, 2))
            atlasFaces.add(face)
            faceTexCoordOnsets.add(texIndex1)

            faces.addAll(
                    vertexIndex1, texIndex1,
                    vertexIndex2, texIndex2,
                    vertexIndex3, texIndex3
            )
            // (1 shl (definition.getFacePriorities()?.get(face)?.toInt()?:definition.getPriority().toInt()))
            faceSmoothingGroups.addAll(0)
        }
    }

    fun rebuildAtlas(){
        val atlasMaterial = createAtlas(model.modelDefinition)
        materialProperty.set(atlasMaterial)
        for (i in atlasFaces.indices) {
            val face = atlasFaces[i]
            val onset = faceTexCoordOnsets[i] * 2
            for (corner in 0..2) {
                texCoords.set(onset + corner * 2, atlasMaterial.getU(face, corner))
                texCoords.set(onset + corner * 2 + 1, atlasMaterial.getV(face, corner))
            }
        }
        syncSurfaceHover()
    }

    private fun syncSurfaceHover() {
        if (!this::meshView.isInitialized)
            return
        meshView.isMouseTransparent = !model.viewModeProperty.get().isDiagnostic()
    }

    /**
     * Create a new [AtlasMaterial].
     */
    private fun createAtlas(definition: ModelDefinition) : AtlasMaterial{
        val atlas = AtlasMaterial()

        val faceCount = definition.getFaceCount()
        if (faceCount == 0)
            return atlas

        val viewMode = model.viewModeProperty.get()
        val recoloredFaces = recolor(definition)
        // Diagnostic view modes replace face colours outright, so shading them would only
        // obscure the very thing they are meant to show.
        val shading = if (model.shadingProperty.get() && !viewMode.isDiagnostic())
            definition.light(faceColors = recoloredFaces)
        else
            null

        atlas.setFaceTints(Array(faceCount) { face ->
            val alpha = model.getRenderFaceAlphas()?.get(face)
            if (shading == null) {
                val color = ModelUtil.hsbToColor(recoloredFaces[face], alpha)
                FaceTint.flat(definition.colorForViewMode(viewMode, face, color))
            } else {
                FaceTint(
                    ModelUtil.hsbToColor(shading.corner1[face], alpha),
                    ModelUtil.hsbToColor(shading.corner2[face], alpha),
                    ModelUtil.hsbToColor(shading.corner3[face], alpha)
                )
            }
        })
        return atlas
    }

    /**
     * Applies the model's find/replace colour mapping, which the client resolves before lighting.
     */
    private fun recolor(definition: ModelDefinition): IntArray {
        val faceColors = definition.getFaceColors()
        val findColor = model.findColor
        val replaceColor = model.replaceColor
        return IntArray(definition.getFaceCount()) { face ->
            var color = faceColors[face]
            if (findColor != null && replaceColor != null) {
                assert(findColor.size == replaceColor.size)
                for (i in findColor.indices) {
                    if (findColor[i] == color)
                        color = replaceColor[i]
                }
            }
            color.toInt() and 0xFFFF
        }
    }
}