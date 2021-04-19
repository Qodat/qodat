package stan.qodat.scene.runescape.model

import javafx.geometry.Bounds
import javafx.scene.paint.Color
import javafx.scene.paint.PhongMaterial
import javafx.scene.shape.*

/**
 * This class represents a [TriangleMesh] that can cache duplicate vertices.
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/09/2019
 * @version 1.0
 */
abstract class ModelMesh : TriangleMesh(), ModelSkin {

    val vertexMap = HashMap<Int, Int>()

    private lateinit var selectionBox: Box

    /**
     * Store the vertex points in this mesh's [points] and then return its onset.
     *
     * This method only computes new coordinates when there is no previous mapping in [vertexMap].
     *
     * @param vertex the vertex index.
     * @param vertexX the x coordinate of the point
     * @param vertexY the y coordinate of the point
     * @param vertexZ the z coordinate of the point
     *
     * @return the onset index of the vertex point coordinates in [points]
     */
    fun addVertex(vertex: Int, vertexX: Int, vertexY: Int, vertexZ: Int) = vertexMap.getOrPut(vertex) {
        val onset = points.size() / 3
        points.addAll(vertexX.toFloat(), vertexY.toFloat(), vertexZ.toFloat())
        onset
    }

    /**
     * Store the UV coordinates in this mesh's [texCoords] and then return its onset.
     *
     * @param u the x coordinate of a pixel in an image.
     * @param v the y coordinate of a pixel in an image.
     *
     * @return the onset index of the UV coordinates in [texCoords]
     */
    fun addUV(u: Float, v: Float): Int {
        val onset = texCoords.size() / 2
        texCoords.addAll(u, v)
        return onset
    }

    /**
     * Clears all assets of this [ModelMesh].
     */
    fun clear(){
        texCoords.clear()
        points.clear()
        faces.clear()
        faceSmoothingGroups.clear()
    }

    /**
     * Compares each vertex coordinate from the [skeleton] to the local [points].
     *
     * Because performing operations on the [points] collection is expensive,
     * we only update the x,y,z coordinates that have changed.
     *
     * @param skeleton the [ModelSkeleton] to retrieve x,y,z values from for each local vertex.
     */
    override fun updatePoints(skeleton: ModelSkeleton){
        for((vertex, localVertex) in vertexMap){
            val x = skeleton.getX(vertex).toFloat()
            val y = skeleton.getY(vertex).toFloat()
            val z = skeleton.getZ(vertex).toFloat()
            (localVertex * 3 ).let { if (points.get(it) != x) points.set(it, x) }
            (localVertex * 3 + 1).let { if (points.get(it) != y) points.set(it, y) }
            (localVertex * 3 + 2).let { if (points.get(it) != z) points.set(it, z) }
        }
    }

    fun getSelectionBox() : Box {
        if (!this::selectionBox.isInitialized){
            selectionBox = Box()
            selectionBox.cullFace = CullFace.BACK
            selectionBox.drawMode = DrawMode.LINE
            selectionBox.material = PhongMaterial(Color.web("#CC7832"))
            val meshView = getSceneNode()
            setSelectionBoxBounds(meshView.boundsInLocal)
            meshView.boundsInLocalProperty().addListener { _, _, newValue ->
                setSelectionBoxBounds(newValue)
            }
        }
        return selectionBox
    }

    private fun setSelectionBoxBounds(newValue: Bounds) {
        selectionBox.translateX = newValue.minX + newValue.width / 2
        selectionBox.translateY = newValue.minY + newValue.height / 2
        selectionBox.translateZ = newValue.minZ + newValue.depth / 2
        selectionBox.width = newValue.width
        selectionBox.height = newValue.height
        selectionBox.depth = newValue.depth
    }

    abstract override fun getSceneNode() : MeshView
}