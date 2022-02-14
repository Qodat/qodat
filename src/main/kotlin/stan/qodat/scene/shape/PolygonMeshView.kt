package stan.qodat.scene.shape

import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ArrayChangeListener
import javafx.collections.ObservableFloatArray
import javafx.scene.Parent
import javafx.scene.paint.Material

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   22/01/2021
 */
class PolygonMeshView(polygonMesh: PolygonMesh) : Parent() {

    private var pointsDirty = true

    private val pointsListener = ArrayChangeListener<ObservableFloatArray> { _, _, _, _ ->
        pointsDirty = true
        updateMesh()
    }

    private var texCoordinatesDirty = true

    private val texCoordinatesListener = ArrayChangeListener<ObservableFloatArray> { _, _, _, _ ->
        texCoordinatesDirty = true
        updateMesh()
    }

    val meshProperty = SimpleObjectProperty(polygonMesh).also { meshProperty ->
        meshProperty.addListener { _, oldValue, newValue ->

            oldValue?.also { oldMesh ->
                oldMesh.points.removeListener(pointsListener)
                oldMesh.texCoordinates.removeListener(texCoordinatesListener)
            }

            meshProperty.set(newValue)
            pointsDirty = true
            texCoordinatesDirty = true
            updateMesh()

            newValue?.also { newMesh ->
                newMesh.points.addListener(pointsListener)
                newMesh.texCoordinates.addListener(texCoordinatesListener)
            }
        }
    }

    val materialProperty = SimpleObjectProperty<Material>()

    private fun updateMesh(){
        val mesh = meshProperty.get()
    }
}