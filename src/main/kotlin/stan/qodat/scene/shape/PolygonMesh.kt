package stan.qodat.scene.shape

import javafx.collections.FXCollections
import stan.qodat.collections.Observable2DIntArrayImpl
import stan.qodat.util.onInvalidation

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   22/01/2021
 */
class PolygonMesh(
    points: FloatArray,
    texCoords: FloatArray,
    faces: Array<IntArray>
) {

    val points = FXCollections.observableFloatArray(*points)
    val texCoordinates = FXCollections.observableFloatArray(*texCoords)
    val faceSmoothingGroups = FXCollections.observableIntegerArray()
    val faces = Observable2DIntArrayImpl(faces)

    private var numberOfEdgesInFaces : Int

    init {
        numberOfEdgesInFaces = getNumberOfEdgesInFaces()
        this.faces.onInvalidation {
            numberOfEdgesInFaces = getNumberOfEdgesInFaces()
        }
    }

    private fun getNumberOfEdgesInFaces() : Int {
        var edges = 0
        for (i in 0 until faces.size())
            edges += faces[i].size
        return edges / 2
    }
}