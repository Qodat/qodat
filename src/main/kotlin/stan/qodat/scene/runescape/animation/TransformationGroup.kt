package stan.qodat.scene.runescape.animation

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import stan.qodat.util.Searchable

/**
 * TODO: add documentation
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   11/02/2021
 */
class TransformationGroup(
    name: String,
    transformationType: TransformationType,
    groupIndices: IntArray
) : Searchable {

    val labelProperty = SimpleStringProperty(name)
    val typeProperty = SimpleObjectProperty(transformationType)
    val groupIndices = FXCollections.observableArrayList(groupIndices.toList())

    override fun getName(): String {
        return labelProperty.get()
    }
}