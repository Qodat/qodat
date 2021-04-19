package stan.qodat.scene.runescape.animation

import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   28/01/2021
 */
class Transformation(
    name: String,
    vertexGroupIndices: IntArray = IntArray(0),
    type: Int = -1,
    deltaX: Int = 0,
    deltaY: Int = 0,
    deltaZ: Int = 0
) {

    val groupIndices = FXCollections.observableIntegerArray(*vertexGroupIndices)

    val typeProperty = SimpleObjectProperty(TransformationType.get(type))
    val deltaXProperty = SimpleIntegerProperty(deltaX)
    val deltaYProperty = SimpleIntegerProperty(deltaY)
    val deltaZProperty = SimpleIntegerProperty(deltaZ)
    val enabledProperty = SimpleBooleanProperty(true)
    val labelProperty = SimpleStringProperty(name)

    fun getType() = typeProperty.get()!!
    fun getDeltaX() = deltaXProperty.get()
    fun getDeltaY() = deltaYProperty.get()
    fun getDeltaZ() = deltaZProperty.get()

    fun clone(): Transformation {
        return Transformation(
            name = labelProperty.get()+"_copy",
            vertexGroupIndices = groupIndices.toArray(null),
            type = getType().ordinal,
            deltaX = getDeltaX(),
            deltaY = getDeltaY(),
            deltaZ = getDeltaZ()
        )
    }
}