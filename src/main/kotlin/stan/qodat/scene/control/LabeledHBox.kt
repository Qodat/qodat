package stan.qodat.scene.control

import javafx.beans.property.SimpleStringProperty
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.HBox

/**
 * Represents a [HBox] containing a [Label] bound to the text property.
 *
 * @author  Stan van der Bend (https://www.rune-server.ee/members/StanDev/)
 * @since   31/01/2021
 */
class LabeledHBox(
    textProperty: SimpleStringProperty,
    spacing: Double = 10.0,
    alignment: Pos = Pos.CENTER_LEFT
) : HBox(spacing) {

    val label = Label()
    init {
        this.alignment = alignment
        label.textProperty().bind(textProperty)
        children.add(label)
    }
}